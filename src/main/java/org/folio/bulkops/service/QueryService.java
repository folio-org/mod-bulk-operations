package org.folio.bulkops.service;

import static java.util.Objects.isNull;
import static org.folio.bulkops.domain.dto.OperationStatusType.CANCELLED;
import static org.folio.bulkops.domain.dto.OperationStatusType.COMPLETED_WITH_ERRORS;
import static org.folio.bulkops.domain.dto.OperationStatusType.DATA_MODIFICATION;
import static org.folio.bulkops.domain.dto.OperationStatusType.FAILED;
import static org.folio.bulkops.domain.dto.OperationStatusType.RETRIEVING_RECORDS;
import static org.folio.bulkops.domain.dto.OperationStatusType.SAVED_IDENTIFIERS;
import static org.folio.bulkops.util.Constants.ERROR_MATCHING_FILE_NAME_PREFIX;
import static org.folio.bulkops.util.Constants.ERROR_STARTING_BULK_OPERATION;
import static org.folio.bulkops.util.Constants.MARC_RECORDS;
import static org.folio.bulkops.util.Constants.MATCHED_RECORDS;
import static org.folio.bulkops.util.Constants.MULTIPLE_SRS;
import static org.folio.bulkops.util.Constants.NEW_LINE_SEPARATOR;
import static org.folio.bulkops.util.Constants.NO_MARC_CONTENT;
import static org.folio.bulkops.util.Constants.SLASH;
import static org.folio.bulkops.util.Constants.SRS_MISSING;
import static org.folio.bulkops.util.Utils.resolveEntityClass;
import static org.folio.spring.scope.FolioExecutionScopeExecutionContextManager.getRunnableWithCurrentFolioContext;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Writer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.StreamSupport;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.folio.bulkops.client.QueryClient;
import org.folio.bulkops.client.RemoteFileSystemClient;
import org.folio.bulkops.client.SrsClient;
import org.folio.bulkops.domain.bean.BulkOperationsEntity;
import org.folio.bulkops.domain.bean.ExtendedHoldingsRecord;
import org.folio.bulkops.domain.bean.ExtendedInstance;
import org.folio.bulkops.domain.bean.ExtendedItem;
import org.folio.bulkops.domain.bean.HoldingsRecord;
import org.folio.bulkops.domain.bean.Instance;
import org.folio.bulkops.domain.bean.Item;
import org.folio.bulkops.domain.bean.StateType;
import org.folio.bulkops.domain.bean.User;
import org.folio.bulkops.domain.converter.JsonToMarcConverter;
import org.folio.bulkops.domain.entity.BulkOperation;
import org.folio.bulkops.domain.entity.BulkOperationExecutionContent;
import org.folio.bulkops.exception.UploadFromQueryException;
import org.folio.bulkops.processor.permissions.check.PermissionsValidator;
import org.folio.bulkops.repository.BulkOperationRepository;
import org.folio.bulkops.util.BulkOperationsEntityCsvWriter;
import org.folio.bulkops.util.CSVHelper;
import org.folio.querytool.domain.dto.QueryDetails;
import org.folio.querytool.domain.dto.SubmitQuery;
import org.folio.spring.FolioExecutionContext;
import org.springframework.stereotype.Service;

@Service
@Log4j2
@RequiredArgsConstructor
public class QueryService {
  public static final String QUERY_FILENAME_TEMPLATE = "%1$s/Query-%1$s.csv";

  private final BulkOperationRepository bulkOperationRepository;
  private final ErrorService errorService;

  private final FolioExecutionContext folioExecutionContext;

  private final ObjectMapper objectMapper;

  private final JsonToMarcConverter jsonToMarcConverter;

  private final PermissionsValidator permissionsValidator;

  private final RemoteFileSystemClient remoteFileSystemClient;
  private final SrsClient srsClient;
  private final QueryClient queryClient;

  private final ExecutorService executor = Executors.newCachedThreadPool();

  public BulkOperation retrieveRecordsAndCheckQueryExecutionStatus(BulkOperation bulkOperation) {
    executor.execute(getRunnableWithCurrentFolioContext(() -> {
      var queryId = executeQuery(new SubmitQuery(bulkOperation.getFqlQuery(), bulkOperation.getEntityTypeId()));
      var queryResult = getResult(queryId);
      switch (queryResult.getStatus()) {
          case IN_PROGRESS -> log.info("Retrieving records by FKM for operation {} in progress...", bulkOperation.getId());
          case SUCCESS -> {
          if (queryResult.getTotalRecords() == 0) {
            failBulkOperation(bulkOperation, "No records found for the query");
          } else {
            saveIdentifiersAndStartBulkOperation(bulkOperation, queryResult);
          }
        }
        case FAILED -> failBulkOperation(bulkOperation, queryResult.getFailureReason());
        case CANCELLED -> cancelBulkOperation((bulkOperation));
      }
    }));
    bulkOperation.setStatus(RETRIEVING_RECORDS);
    bulkOperationRepository.save(bulkOperation);
    return bulkOperation;
  }

  private UUID executeQuery(SubmitQuery submitQuery) {
    return queryClient.executeQuery(submitQuery).getQueryId();
  }

  private QueryDetails getResult(UUID queryId) {
    return queryClient.getQuery(queryId, true);
  }

  private void saveIdentifiersAndStartBulkOperation(BulkOperation bulkOperation, QueryDetails queryResult) {
    try {
      var identifiers = retrieveIdentifiers(queryResult);
      var identifiersString = String.join(NEW_LINE_SEPARATOR, identifiers);
      var path = String.format(QUERY_FILENAME_TEMPLATE, bulkOperation.getId());
      remoteFileSystemClient.put(new ByteArrayInputStream(identifiersString.getBytes()), path);
      bulkOperation.setLinkToTriggeringCsvFile(path);
      bulkOperation.setStatus(SAVED_IDENTIFIERS);
      bulkOperation.setTotalNumOfRecords(identifiers.size());
      bulkOperationRepository.save(bulkOperation);
      startQueryOperation(queryResult, bulkOperation);
    } catch (Exception e) {
      var errorMessage = "Failed to save identifiers, reason: " + e.getMessage();
      log.error(errorMessage);
      failBulkOperation(bulkOperation, errorMessage);
    }
  }

  private void startQueryOperation(QueryDetails queryResult, BulkOperation operation) {
    try {
      var matchedJsonFileName = operation.getId() + SLASH + "json" + SLASH + LocalDate.now() + MATCHED_RECORDS + FilenameUtils.getBaseName(operation.getLinkToTriggeringCsvFile()) + ".json";
      var matchedMrcFileName = operation.getId() + SLASH + LocalDate.now() + MARC_RECORDS + FilenameUtils.getBaseName(operation.getLinkToTriggeringCsvFile());
      var matchedCsvFileName = operation.getId() + SLASH + LocalDate.now() + MATCHED_RECORDS + FilenameUtils.getBaseName(operation.getLinkToTriggeringCsvFile()) + ".csv";

      operation.setStatus(RETRIEVING_RECORDS);
      bulkOperationRepository.save(operation);

      processQueryResult(queryResult, matchedCsvFileName, matchedJsonFileName, matchedMrcFileName, operation);

      if (operation.getMatchedNumOfRecords() > 0) {
        operation.setLinkToMatchedRecordsCsvFile(matchedCsvFileName);
        operation.setLinkToMatchedRecordsJsonFile(matchedJsonFileName);
        operation.setStatus(DATA_MODIFICATION);
      } else {
        operation.setStatus(COMPLETED_WITH_ERRORS);
      }
      operation.setEndTime(LocalDateTime.now());
      bulkOperationRepository.save(operation);
    } catch (Exception e) {
      log.error(ERROR_STARTING_BULK_OPERATION, e);
      operation.setStatus(FAILED);
      operation.setErrorMessage(e.getMessage());
      operation.setEndTime(LocalDateTime.now());
      var linkToMatchingErrorsFile = errorService.uploadErrorsToStorage(operation.getId(), ERROR_MATCHING_FILE_NAME_PREFIX, e.getMessage());
      operation.setLinkToMatchedRecordsErrorsCsvFile(linkToMatchingErrorsFile);
    }
  }

  private void processQueryResult(QueryDetails queryDetails, String matchedCsvFileName, String matchedJsonFileName,
                                  String matchedMrcFileName, BulkOperation operation) throws IOException, CsvRequiredFieldEmptyException, CsvDataTypeMismatchException {
    try (var writerForResultCsvFile = remoteFileSystemClient.writer(matchedCsvFileName);
         var writerForResultJsonFile = remoteFileSystemClient.writer(matchedJsonFileName);
         var writerForResultMrcFile = remoteFileSystemClient.writer(matchedMrcFileName)) {
      var entityClass = resolveEntityClass(operation.getEntityType());
      var csvWriter = new BulkOperationsEntityCsvWriter(writerForResultCsvFile, entityClass);
      List<BulkOperationExecutionContent> bulkOperationExecutionContents = new ArrayList<>();

      int numMatched = 0;
      int numProcessed = 0;
      for (var content : queryDetails.getContent()) {

        var entityRecord = constructRecord(operation, content);

        try {
          permissionsValidator.checkPermissions(operation, entityRecord);

          var extendedRecord = constructExtendedRecord(operation, entityRecord);

          if (entityRecord.isMarcInstance()) {
            processQueryResultForMarc(entityRecord, writerForResultMrcFile, operation, matchedMrcFileName);
          }

          var extendedRecordAsJsonString = objectMapper.writeValueAsString(extendedRecord);

          writerForResultJsonFile.append(extendedRecordAsJsonString);
          CSVHelper.writeBeanToCsv(operation, csvWriter, extendedRecord.getRecordBulkOperationEntity(), bulkOperationExecutionContents);
          numMatched++;
        } catch (UploadFromQueryException e) {
          handleError(bulkOperationExecutionContents, e, entityRecord, operation);
        }
        updateProgress(operation, numMatched, ++numProcessed);
      }
      errorService.saveErrorsAfterQuery(bulkOperationExecutionContents, operation);
    }
  }

  private void updateProgress(BulkOperation operation, int numMatched, int numProcessed) {
    operation.setProcessedNumOfRecords(numProcessed);
    operation.setMatchedNumOfRecords(numMatched);
    bulkOperationRepository.save(operation);
  }

  private BulkOperation failBulkOperation(BulkOperation bulkOperation, String errorMessage) {
    bulkOperation.setStatus(FAILED);
    bulkOperation.setErrorMessage(errorMessage);
    bulkOperation.setEndTime(LocalDateTime.now());
    bulkOperationRepository.save(bulkOperation);
    return bulkOperation;
  }

  private void cancelBulkOperation(BulkOperation bulkOperation) {
    bulkOperation.setStatus(CANCELLED);
    bulkOperation.setErrorMessage("Query execution was cancelled");
    bulkOperation.setEndTime(LocalDateTime.now());
    bulkOperationRepository.save(bulkOperation);
  }

  private List<String> retrieveIdentifiers(QueryDetails queryDetails) {
    return queryDetails.getContent().stream().map(content -> content.get("instance.id").toString()).sorted().distinct().toList();
  }

  private void processQueryResultForMarc(BulkOperationsEntity entityRecord, Writer writerForResultMrcFile,
                                         BulkOperation operation, String matchedMrcFileName) throws UploadFromQueryException {
    try {
      var listMarcRecords = getMarcContent(entityRecord.getId());
      writerForResultMrcFile.append(listMarcRecords.getFirst());
      if (isNull(operation.getLinkToMatchedRecordsMarcFile())) {
        operation.setLinkToMatchedRecordsMarcFile(matchedMrcFileName);
      }
    } catch (UploadFromQueryException e) {
      throw e;
    } catch (Exception e) {
      throw new UploadFromQueryException(NO_MARC_CONTENT.formatted(entityRecord.getId(), e.getMessage()), entityRecord.getId());
    }
  }

  private void handleError(List<BulkOperationExecutionContent> bulkOperationExecutionContents, Exception e,
                           BulkOperationsEntity entityRecord, BulkOperation operation) {
    log.error(e);
    bulkOperationExecutionContents.add(BulkOperationExecutionContent.builder()
      .identifier(entityRecord.getId())
      .bulkOperationId(operation.getId())
      .state(StateType.FAILED)
      .errorType(org.folio.bulkops.domain.dto.ErrorType.ERROR)
      .errorMessage(e.getMessage())
      .build());
  }

  private BulkOperationsEntity constructRecord(BulkOperation operation, Map<String, Object> content) throws JsonProcessingException {
    return switch(operation.getEntityType()) {
      case USER -> constructRecord(content,"users.jsonb", User.class);
      case ITEM -> constructRecord(content,"items.jsonb", Item.class);
      case HOLDINGS_RECORD -> constructRecord(content,"holdings.jsonb", HoldingsRecord.class);
      case INSTANCE, INSTANCE_MARC -> constructRecord(content,"instance.jsonb", Instance.class);
    };
  }

  private BulkOperationsEntity constructRecord(Map<String, Object> content, String jsonb, Class<? extends BulkOperationsEntity> recordClass) throws JsonProcessingException {
    Map<String, String> recordJsonbAsMap = objectMapper.readValue((String)content.get(jsonb), Map.class);
    return objectMapper.convertValue(recordJsonbAsMap, recordClass);
  }

  private BulkOperationsEntity constructExtendedRecord(BulkOperation operation, BulkOperationsEntity entityRecord) {
    Map<String, Object> extendedRecordAsMap = new LinkedHashMap<>();
    extendedRecordAsMap.put("tenantId", folioExecutionContext.getTenantId());
    extendedRecordAsMap.put("entity", entityRecord);
    return switch(operation.getEntityType()) {
      case USER -> objectMapper.convertValue(entityRecord, User.class);
      case ITEM -> objectMapper.convertValue(extendedRecordAsMap, ExtendedItem.class);
      case HOLDINGS_RECORD -> objectMapper.convertValue(extendedRecordAsMap, ExtendedHoldingsRecord.class);
      case INSTANCE, INSTANCE_MARC -> objectMapper.convertValue(extendedRecordAsMap, ExtendedInstance.class);
    };
  }

  private List<String> getMarcContent(String id) throws UploadFromQueryException, IOException {
    List<String> mrcRecords = new ArrayList<>();
    var srsRecords = srsClient.getMarc(id, "INSTANCE", true).get("sourceRecords");
    if (srsRecords.isEmpty()) {
      throw new UploadFromQueryException(SRS_MISSING);
    }
    if (srsRecords.size() > 1) {
      throw new UploadFromQueryException(MULTIPLE_SRS.formatted(String.join(", ", getAllSrsIds(srsRecords))));
    }
    for (var jsonNodeIterator = srsRecords.elements(); jsonNodeIterator.hasNext();) {
      var srsRec = jsonNodeIterator.next();
      var parsedRec = srsRec.get("parsedRecord");
      var content = parsedRec.get("content").toString();
      mrcRecords.add(jsonToMarcConverter.convertJsonRecordToMarcRecord(content));
    }
    return mrcRecords;
  }

  private String getAllSrsIds(JsonNode srsRecords) {
    return String.join(", ", StreamSupport.stream(srsRecords.spliterator(), false)
      .map(n -> StringUtils.strip(n.get("recordId").toString(), "\"")).toList());
  }
}
