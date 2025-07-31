package org.folio.bulkops.service;

import static java.util.Objects.isNull;
import static org.folio.bulkops.domain.dto.OperationStatusType.CANCELLED;
import static org.folio.bulkops.domain.dto.OperationStatusType.COMPLETED_WITH_ERRORS;
import static org.folio.bulkops.domain.dto.OperationStatusType.DATA_MODIFICATION;
import static org.folio.bulkops.domain.dto.OperationStatusType.FAILED;
import static org.folio.bulkops.domain.dto.OperationStatusType.RETRIEVING_RECORDS;
import static org.folio.bulkops.util.Constants.ERROR_MATCHING_FILE_NAME_PREFIX;
import static org.folio.bulkops.util.Constants.ERROR_STARTING_BULK_OPERATION;
import static org.folio.bulkops.util.Constants.MULTIPLE_SRS;
import static org.folio.bulkops.util.Constants.NEW_LINE_SEPARATOR;
import static org.folio.bulkops.util.Constants.NO_MARC_CONTENT;
import static org.folio.bulkops.util.Constants.SRS_MISSING;
import static org.folio.bulkops.util.Utils.getMatchedFileName;
import static org.folio.bulkops.util.Utils.resolveEntityClass;
import static org.folio.bulkops.util.Utils.resolveExtendedEntityClass;
import static org.folio.spring.scope.FolioExecutionScopeExecutionContextManager.getRunnableWithCurrentFolioContext;

import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.StreamSupport;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.folio.bulkops.client.QueryClient;
import org.folio.bulkops.client.RemoteFileSystemClient;
import org.folio.bulkops.client.SrsClient;
import org.folio.bulkops.domain.bean.BulkOperationsEntity;
import org.folio.bulkops.domain.bean.HoldingsRecord;
import org.folio.bulkops.domain.bean.Item;
import org.folio.bulkops.domain.bean.StateType;
import org.folio.bulkops.domain.converter.JsonToMarcConverter;
import org.folio.bulkops.domain.entity.BulkOperation;
import org.folio.bulkops.domain.entity.BulkOperationExecutionContent;
import org.folio.bulkops.exception.UploadFromQueryException;
import org.folio.bulkops.processor.permissions.check.PermissionsValidator;
import org.folio.bulkops.repository.BulkOperationRepository;
import org.folio.bulkops.util.BulkOperationsEntityCsvWriter;
import org.folio.bulkops.util.CSVHelper;
import org.folio.bulkops.util.FqmContentFetcher;
import org.springframework.stereotype.Service;

@Service
@Log4j2
@RequiredArgsConstructor
public class QueryService {
  public static final String QUERY_FILENAME_TEMPLATE = "%1$s/Query-%1$s.csv";
  private static final int STATISTICS_UPDATING_STEP = 100;

  private final BulkOperationRepository bulkOperationRepository;
  private final ErrorService errorService;
  private final ObjectMapper objectMapper;
  private final JsonToMarcConverter jsonToMarcConverter;
  private final PermissionsValidator permissionsValidator;
  private final RemoteFileSystemClient remoteFileSystemClient;
  private final SrsClient srsClient;
  private final QueryClient queryClient;
  private final FqmContentFetcher fqmContentFetcher;
  private final LocalReferenceDataService localReferenceDataService;

  private final ExecutorService executor = Executors.newCachedThreadPool();

  public BulkOperation retrieveRecordsAndCheckQueryExecutionStatus(BulkOperation bulkOperation) {
    executor.execute(getRunnableWithCurrentFolioContext(() -> {
      var queryResult = queryClient.getQuery(bulkOperation.getFqlQueryId(), true);
      switch (queryResult.getStatus()) {
          case IN_PROGRESS -> log.info("Retrieving records by FQM for operation {} in progress...", bulkOperation.getId());
          case SUCCESS -> {
          if (queryResult.getTotalRecords() == 0) {
            failBulkOperation(bulkOperation, "No records found for the query");
          } else {
            bulkOperation.setTotalNumOfRecords(queryResult.getTotalRecords());
            List<BulkOperationExecutionContent> bulkOperationExecutionContents = new ArrayList<>();
            try (var is = fqmContentFetcher.fetch(bulkOperation.getFqlQueryId(), bulkOperation.getEntityType(), queryResult.getTotalRecords(), bulkOperationExecutionContents, bulkOperation.getId())) {
              startQueryOperation(is, bulkOperation, bulkOperationExecutionContents);
            } catch (Exception e) {
              var errorMessage = "Failed to save identifiers, reason: " + e.getMessage();
              log.error(errorMessage);
              failBulkOperation(bulkOperation, errorMessage);
            }
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

  private void startQueryOperation(InputStream is, BulkOperation operation, List<BulkOperationExecutionContent> bulkOperationExecutionContents) {
    try {
      var triggeringCsvFileName = String.format(QUERY_FILENAME_TEMPLATE, operation.getId());
      var matchedJsonFileName = getMatchedFileName(operation.getId(), "json/", "Matched", triggeringCsvFileName, "json");
      var matchedMrcFileName = getMatchedFileName(operation.getId(), StringUtils.EMPTY, "Marc", triggeringCsvFileName, "mrc");
      var matchedCsvFileName = getMatchedFileName(operation.getId(), StringUtils.EMPTY, "Matched", triggeringCsvFileName, "csv");

      operation.setStatus(RETRIEVING_RECORDS);
      operation.setLinkToTriggeringCsvFile(triggeringCsvFileName);
      bulkOperationRepository.save(operation);

      processQueryResult(is, triggeringCsvFileName, matchedCsvFileName, matchedJsonFileName, matchedMrcFileName, operation, bulkOperationExecutionContents);

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

  private void processQueryResult(InputStream is, String triggeringCsvFileName, String matchedCsvFileName, String matchedJsonFileName,
                                  String matchedMrcFileName, BulkOperation operation, List<BulkOperationExecutionContent> bulkOperationExecutionContents) throws IOException, CsvRequiredFieldEmptyException, CsvDataTypeMismatchException {
    try (var writerForTriggeringCsvFile = remoteFileSystemClient.writer(triggeringCsvFileName);
         var writerForResultCsvFile = remoteFileSystemClient.writer(matchedCsvFileName);
         var writerForResultJsonFile = remoteFileSystemClient.writer(matchedJsonFileName);
         var writerForResultMrcFile = remoteFileSystemClient.writer(matchedMrcFileName)) {
      var entityClass = resolveEntityClass(operation.getEntityType());
      var extendedEntityClass = resolveExtendedEntityClass(operation.getEntityType());
      var csvWriter = new BulkOperationsEntityCsvWriter(writerForResultCsvFile, entityClass);

      int numMatched = 0;
      int numProcessed = 0;
      var factory = objectMapper.getFactory();
      var parser = factory.createParser(is);
      var iterator = objectMapper.readValues(parser, extendedEntityClass);
      Set<String> usedTenants = new HashSet<>();
      while (iterator.hasNext()) {

        var extendedRecord = iterator.next();
        if (extendedRecord.getRecordBulkOperationEntity() instanceof Item item) {
          localReferenceDataService.enrichWithTenant(item, extendedRecord.getTenant());
        }
        if (extendedRecord.getRecordBulkOperationEntity() instanceof HoldingsRecord holdingsRecord) {
          localReferenceDataService.enrichWithTenant(holdingsRecord, extendedRecord.getTenant());
        }

        usedTenants.add(extendedRecord.getTenant());

        try {
          permissionsValidator.checkPermissions(operation, extendedRecord);

          if (extendedRecord.isMarcInstance()) {
            processQueryResultForMarc(extendedRecord.getRecordBulkOperationEntity(), writerForResultMrcFile, operation, matchedMrcFileName);
          }

          var extendedRecordAsJsonString = objectMapper.writeValueAsString(extendedRecord);

          writerForResultJsonFile.append(extendedRecordAsJsonString);
          CSVHelper.writeBeanToCsv(operation, csvWriter, extendedRecord.getRecordBulkOperationEntity(), bulkOperationExecutionContents);
          numMatched++;
        } catch (UploadFromQueryException e) {
          handleError(bulkOperationExecutionContents, e, extendedRecord.getRecordBulkOperationEntity(), operation);
        } finally {
          writerForTriggeringCsvFile.write(extendedRecord.getRecordBulkOperationEntity().getId() + NEW_LINE_SEPARATOR);
        }
        ++numProcessed;

        if (numProcessed % STATISTICS_UPDATING_STEP == 0) {
          updateOperationExecutionStatus(operation, numProcessed, numMatched);
        }
      }
      operation.setUsedTenants(new ArrayList<>(usedTenants));
      if (numProcessed % STATISTICS_UPDATING_STEP != 0) {
        updateOperationExecutionStatus(operation, numProcessed, numMatched);
      }
      errorService.saveErrorsAfterQuery(bulkOperationExecutionContents, operation);
    }
  }

  private void updateOperationExecutionStatus(BulkOperation operation, int numProcessed, int numMatched) {
    operation.setProcessedNumOfRecords(numProcessed);
    operation.setMatchedNumOfRecords(numMatched);
    bulkOperationRepository.save(operation);
  }

  private void failBulkOperation(BulkOperation bulkOperation, String errorMessage) {
    bulkOperation.setStatus(FAILED);
    bulkOperation.setErrorMessage(errorMessage);
    bulkOperation.setEndTime(LocalDateTime.now());
    bulkOperationRepository.save(bulkOperation);
  }

  private void cancelBulkOperation(BulkOperation bulkOperation) {
    bulkOperation.setStatus(CANCELLED);
    bulkOperation.setErrorMessage("Query execution was cancelled");
    bulkOperation.setEndTime(LocalDateTime.now());
    bulkOperationRepository.save(bulkOperation);
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
