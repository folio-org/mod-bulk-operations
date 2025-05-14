package org.folio.bulkops.service;

import static java.lang.String.format;
import static java.util.Objects.isNull;
import static org.folio.bulkops.domain.dto.ApproachType.MANUAL;
import static org.folio.bulkops.domain.dto.BulkOperationStep.UPLOAD;
import static org.folio.bulkops.domain.dto.EntityType.HOLDINGS_RECORD;
import static org.folio.bulkops.domain.dto.EntityType.ITEM;
import static org.folio.bulkops.domain.dto.EntityType.INSTANCE_MARC;
import static org.folio.bulkops.domain.dto.EntityType.USER;
import static org.folio.bulkops.domain.dto.EntityType.INSTANCE;
import static org.folio.bulkops.domain.dto.OperationStatusType.DATA_MODIFICATION;
import static org.folio.bulkops.domain.dto.OperationStatusType.NEW;
import static org.folio.bulkops.domain.dto.OperationStatusType.RETRIEVING_IDENTIFIERS;
import static org.folio.bulkops.domain.dto.OperationStatusType.RETRIEVING_RECORDS;
import static org.folio.bulkops.domain.dto.OperationStatusType.SAVED_IDENTIFIERS;
import static org.folio.bulkops.service.BulkOperationService.ERROR_STARTING_BULK_OPERATION;
import static org.folio.bulkops.service.BulkOperationService.FILE_UPLOADING_FAILED;
import static org.folio.bulkops.service.BulkOperationService.STEP_IS_NOT_APPLICABLE_FOR_BULK_OPERATION_STATUS;
import static org.folio.bulkops.util.CSVHelper.writeBeanToCsv;
import static org.folio.bulkops.util.Constants.LINKED_DATA_SOURCE;
import static org.folio.bulkops.util.Constants.LINKED_DATA_SOURCE_IS_NOT_SUPPORTED;
import static org.folio.bulkops.util.Constants.MARC_RECORDS;
import static org.folio.bulkops.util.Constants.MATCHED_RECORDS;
import static org.folio.bulkops.util.Constants.MULTIPLE_SRS;
import static org.folio.bulkops.util.Constants.NEW_LINE_SEPARATOR;
import static org.folio.bulkops.util.Constants.NO_INSTANCE_VIEW_PERMISSIONS;
import static org.folio.bulkops.util.Constants.NO_MARC_CONTENT;
import static org.folio.bulkops.util.Constants.NO_USER_VIEW_PERMISSIONS;
import static org.folio.bulkops.util.Constants.SLASH;
import static org.folio.bulkops.util.Constants.SRS_MISSING;
import static org.folio.bulkops.util.ErrorCode.ERROR_MESSAGE_PATTERN;
import static org.folio.bulkops.util.Utils.resolveEntityClass;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.folio.bulkops.client.RemoteFileSystemClient;
import org.folio.bulkops.client.SrsClient;
import org.folio.bulkops.client.UserClient;
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
import org.folio.bulkops.domain.dto.ErrorType;
import org.folio.bulkops.domain.dto.BulkOperationStep;
import org.folio.bulkops.domain.dto.ApproachType;
import org.folio.bulkops.domain.entity.BulkOperation;
import org.folio.bulkops.domain.entity.BulkOperationExecutionContent;
import org.folio.bulkops.exception.BadRequestException;
import org.folio.bulkops.exception.ReadPermissionException;
import org.folio.bulkops.exception.UploadFromQueryException;
import org.folio.bulkops.processor.permissions.check.HoldingsRecordPermissionChecker;
import org.folio.bulkops.processor.permissions.check.ItemPermissionChecker;
import org.folio.bulkops.processor.permissions.check.PermissionsValidator;
import org.folio.bulkops.repository.BulkOperationRepository;
import org.folio.bulkops.util.BulkOperationsEntityCsvWriter;
import org.folio.querytool.domain.dto.QueryDetails;
import org.folio.querytool.domain.dto.SubmitQuery;
import org.folio.spring.FolioExecutionContext;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.Writer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.StreamSupport;

@Service
@Log4j2
@RequiredArgsConstructor
public class QueryDataRetrieverService {

  private final QueryService queryService;
  private final ErrorService errorService;
  private final HoldingsRecordPermissionChecker holdingsRecordPermissionChecker;
  private final ItemPermissionChecker itemPermissionChecker;

  private final BulkOperationRepository bulkOperationRepository;

  private final FolioExecutionContext folioExecutionContext;

  private final ObjectMapper objectMapper;

  private final JsonToMarcConverter jsonToMarcConverter;

  private final PermissionsValidator permissionsValidator;

  private final RemoteFileSystemClient remoteFileSystemClient;
  private final SrsClient srsClient;
  private final UserClient userClient;

  public String retrieveDataByQueryAndUpdateBulkOperation(BulkOperationStep step, ApproachType approach, BulkOperation operation) {
    String errorMessage = null;
    try {
      var queryId = queryService.executeQuery(new SubmitQuery(operation.getFqlQuery(), operation.getEntityTypeId()));

      operation.setStatus(RETRIEVING_IDENTIFIERS);
      bulkOperationRepository.save(operation);
      var queryDetails = queryService.getResult(queryId);
      queryService.checkQueryExecutionStatus(operation, queryDetails);

      var matchedJsonFileName = operation.getId() + SLASH + "json" + SLASH + LocalDate.now() + MATCHED_RECORDS + FilenameUtils.getBaseName(operation.getLinkToTriggeringCsvFile()) + ".json";
      var matchedMrcFileName = operation.getId() + SLASH + LocalDate.now() + MARC_RECORDS + FilenameUtils.getBaseName(operation.getLinkToTriggeringCsvFile());
      var matchedCsvFileName = operation.getId() + SLASH + LocalDate.now() + MATCHED_RECORDS + FilenameUtils.getBaseName(operation.getLinkToTriggeringCsvFile()) + ".csv";

      operation.setStatus(RETRIEVING_RECORDS);
      bulkOperationRepository.save(operation);
      processQueryResult(queryDetails, matchedCsvFileName, matchedJsonFileName, matchedMrcFileName, operation);

      operation.setLinkToMatchedRecordsCsvFile(matchedCsvFileName);
      operation.setLinkToMatchedRecordsJsonFile(matchedJsonFileName);
      operation.setStatus(DATA_MODIFICATION);
      operation.setEndTime(LocalDateTime.now());
      bulkOperationRepository.save(operation);
    } catch (Exception e) {
      log.error(ERROR_STARTING_BULK_OPERATION, e);
      errorMessage = format(ERROR_MESSAGE_PATTERN, FILE_UPLOADING_FAILED, e.getMessage());
    }
    return errorMessage;
  }

  private void updateProgress(BulkOperation operation, int numMatched, int numProcessed) {
    operation.setProcessedNumOfRecords(numProcessed);
    operation.setMatchedNumOfRecords(numMatched);
    bulkOperationRepository.save(operation);
  }

  private void processQueryResult(QueryDetails queryDetails, String matchedCsvFileName, String matchedJsonFileName,
                                  String matchedMrcFileName, BulkOperation operation) throws IOException, CsvRequiredFieldEmptyException, CsvDataTypeMismatchException {
    try (var writerForResultCsvFile = remoteFileSystemClient.writer(matchedCsvFileName);
         var writerForResultJsonFile = remoteFileSystemClient.writer(matchedJsonFileName);
         var writerForResultMrcFile = remoteFileSystemClient.writer(matchedMrcFileName)) {
      var entityClass = resolveEntityClass(operation.getEntityType());
      var csvWriter = new BulkOperationsEntityCsvWriter(writerForResultCsvFile, entityClass);
      List<BulkOperationExecutionContent> bulkOperationExecutionContents = new ArrayList<>();

      int numMatched = 0, numProcessed = 0;
      for (var content : queryDetails.getContent()) {

        var record = constructRecord(operation, content);

        try {
          checkPermissions(operation, record);

          var extendedRecord = constructExtendedRecord(operation, record);

          if (record.isMarcInstance()) {
            processQueryResultForMarc(record, writerForResultMrcFile, operation, matchedMrcFileName);
          }

          var extendedRecordAsJsonString = objectMapper.writeValueAsString(extendedRecord);

          writerForResultJsonFile.append(extendedRecordAsJsonString);
          writeBeanToCsv(operation, csvWriter, extendedRecord.getRecordBulkOperationEntity(), bulkOperationExecutionContents);
          numMatched ++;
        } catch (UploadFromQueryException e) {
          handleError(bulkOperationExecutionContents, e, record, operation);
        }
        updateProgress(operation, numMatched, ++numProcessed);
      }
      errorService.saveErrorsAfterQuery(bulkOperationExecutionContents, operation);
    }
  }

  private void checkPermissions(BulkOperation operation, BulkOperationsEntity record) throws UploadFromQueryException {
    if (Set.of(USER, INSTANCE, INSTANCE_MARC).contains(operation.getEntityType())) {
      if (!permissionsValidator.isBulkEditReadPermissionExists(folioExecutionContext.getTenantId(), operation.getEntityType())) {
        var user = userClient.getUserById(folioExecutionContext.getUserId().toString());
        throw new ReadPermissionException(buildReadPermissionErrorMessage(operation, record.getId(), user), record.getId());
      }
      if (LINKED_DATA_SOURCE.equals(record.getSource())) {
        throw new UploadFromQueryException(LINKED_DATA_SOURCE_IS_NOT_SUPPORTED, record.getId());
      }
    } else if (HOLDINGS_RECORD == operation.getEntityType()) {
      holdingsRecordPermissionChecker.checkPermissionsAndAffiliations(record.getId());
    } else if (ITEM == operation.getEntityType()) {
      itemPermissionChecker.checkPermissionsAndAffiliations(record.getId());
    }
  }

  private String buildReadPermissionErrorMessage(BulkOperation operation, String identifier, User user) {
    return switch (operation.getEntityType()) {
      case USER -> NO_USER_VIEW_PERMISSIONS.formatted(user.getUsername(), "id", identifier, folioExecutionContext.getTenantId());
      case INSTANCE, INSTANCE_MARC -> NO_INSTANCE_VIEW_PERMISSIONS.formatted(user.getUsername(), "id", identifier, folioExecutionContext.getTenantId());
      default -> throw new IllegalArgumentException("For %s this error message builder cannot be used.".formatted(operation.getEntityType()));
    };
  }

  private void processQueryResultForMarc(BulkOperationsEntity record, Writer writerForResultMrcFile,
                                         BulkOperation operation, String matchedMrcFileName) throws UploadFromQueryException {
    try {
      var listMarcRecords = getMarcContent(record.getId());
      writerForResultMrcFile.append(listMarcRecords.getFirst());
      if (isNull(operation.getLinkToMatchedRecordsMarcFile())) {
        operation.setLinkToMatchedRecordsMarcFile(matchedMrcFileName);
      }
    } catch (UploadFromQueryException e) {
      throw e;
    } catch (Exception e) {
      throw new UploadFromQueryException(NO_MARC_CONTENT.formatted(record.getId(), e.getMessage()), record.getId());
    }
  }

  private void handleError(List<BulkOperationExecutionContent> bulkOperationExecutionContents, Exception e,
                                    BulkOperationsEntity record, BulkOperation operation) {
    log.error(e);
    bulkOperationExecutionContents.add(BulkOperationExecutionContent.builder()
      .identifier(record.getId())
      .bulkOperationId(operation.getId())
      .state(StateType.FAILED)
      .errorType(ErrorType.ERROR)
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

  private BulkOperationsEntity constructExtendedRecord(BulkOperation operation, BulkOperationsEntity record) {
    Map<String, Object> extendedRecordAsMap = new LinkedHashMap<>();
    extendedRecordAsMap.put("tenantId", folioExecutionContext.getTenantId());
    extendedRecordAsMap.put("entity", record);
    return switch(operation.getEntityType()) {
      case USER -> objectMapper.convertValue(record, User.class);
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
