package org.folio.bulkops.service;

import static java.util.Objects.isNull;
import static java.util.UUID.fromString;
import static org.folio.bulkops.domain.dto.ApproachType.QUERY;
import static org.folio.bulkops.domain.dto.OperationStatusType.CANCELLED;
import static org.folio.bulkops.domain.dto.OperationStatusType.COMPLETED_WITH_ERRORS;
import static org.folio.bulkops.domain.dto.OperationStatusType.DATA_MODIFICATION;
import static org.folio.bulkops.domain.dto.OperationStatusType.FAILED;
import static org.folio.bulkops.domain.dto.OperationStatusType.RETRIEVING_RECORDS;
import static org.folio.bulkops.domain.dto.OperationStatusType.SAVED_IDENTIFIERS;
import static org.folio.bulkops.util.Constants.ERROR_MATCHING_FILE_NAME_PREFIX;
import static org.folio.bulkops.util.Constants.ERROR_STARTING_BULK_OPERATION;
import static org.folio.bulkops.util.Constants.NEW_LINE_SEPARATOR;
import static org.folio.bulkops.util.Constants.NO_MARC_CONTENT;
import static org.folio.bulkops.util.Constants.NO_MATCH_FOUND_MESSAGE;
import static org.folio.bulkops.util.Utils.getMatchedFileName;
import static org.folio.bulkops.util.Utils.resolveEntityClass;
import static org.folio.bulkops.util.Utils.resolveExtendedEntityClass;
import static org.folio.spring.scope.FolioExecutionScopeExecutionContextManager.getRunnableWithCurrentFolioContext;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.SetUtils;
import org.apache.commons.lang3.StringUtils;
import org.folio.bulkops.client.QueryClient;
import org.folio.bulkops.client.RemoteFileSystemClient;
import org.folio.bulkops.domain.bean.BulkOperationsEntity;
import org.folio.bulkops.domain.bean.HoldingsRecord;
import org.folio.bulkops.domain.bean.Item;
import org.folio.bulkops.domain.bean.StateType;
import org.folio.bulkops.domain.dto.ApproachType;
import org.folio.bulkops.domain.dto.ErrorType;
import org.folio.bulkops.domain.entity.BulkOperation;
import org.folio.bulkops.domain.entity.BulkOperationExecutionContent;
import org.folio.bulkops.exception.MarcValidationException;
import org.folio.bulkops.exception.UploadFromQueryException;
import org.folio.bulkops.processor.permissions.check.PermissionsValidator;
import org.folio.bulkops.repository.BulkOperationRepository;
import org.folio.bulkops.util.BulkOperationsEntityCsvWriter;
import org.folio.bulkops.util.CsvHelper;
import org.folio.bulkops.util.FqmContentFetcher;
import org.folio.querytool.domain.dto.QueryDetails;
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
  private final PermissionsValidator permissionsValidator;
  private final RemoteFileSystemClient remoteFileSystemClient;
  private final QueryClient queryClient;
  private final FqmContentFetcher fqmContentFetcher;
  private final LocalReferenceDataService localReferenceDataService;
  private final SrsService srsService;

  private final ExecutorService executor = Executors.newCachedThreadPool();

  public void retrieveRecordsIdentifiersFlowAsync(
      List<UUID> uuids,
      BulkOperation bulkOperation,
      List<BulkOperationExecutionContent> bulkOperationExecutionContents) {
    executor.execute(
        getRunnableWithCurrentFolioContext(
            () -> {
              try (var is =
                  fqmContentFetcher.contents(
                      uuids,
                      bulkOperation.getEntityType(),
                      bulkOperationExecutionContents,
                      bulkOperation.getId())) {
                completeBulkOperation(
                    is, bulkOperation, new HashSet<>(uuids), bulkOperationExecutionContents);
              } catch (Exception e) {
                var errorMessage =
                    "Failed to save identifiers (FQM-based Identifiers Flow), reason: "
                        + e.getMessage();
                log.error(errorMessage);
                failAndSaveBulkOperation(bulkOperation, errorMessage);
              }
            }));
    bulkOperation.setStatus(RETRIEVING_RECORDS);
    bulkOperationRepository.save(bulkOperation);
  }

  public BulkOperation retrieveRecordsQueryFlowAsync(BulkOperation bulkOperation) {
    executor.execute(
        getRunnableWithCurrentFolioContext(
            () -> {
              var queryResult = getQueryResult(bulkOperation);
              switch (queryResult.getStatus()) {
                case IN_PROGRESS ->
                    log.info(
                        "Retrieving records by FQM for operation {} in progress...",
                        bulkOperation.getId());
                case SUCCESS -> {
                  if (queryResult.getTotalRecords() == 0) {
                    failAndSaveBulkOperation(bulkOperation, "No records found for the query");
                  } else {
                    bulkOperation.setStatus(RETRIEVING_RECORDS);
                    bulkOperation.setTotalNumOfRecords(queryResult.getTotalRecords());
                    List<BulkOperationExecutionContent> bulkOperationExecutionContents =
                        new ArrayList<>();
                    try (var is =
                        fqmContentFetcher.fetch(
                            bulkOperation.getFqlQueryId(),
                            bulkOperation.getEntityType(),
                            queryResult.getTotalRecords(),
                            bulkOperationExecutionContents,
                            bulkOperation.getId())) {
                      completeBulkOperation(
                          is, bulkOperation, Set.of(), bulkOperationExecutionContents);
                    } catch (Exception e) {
                      var errorMessage =
                          "Failed to save identifiers (FQM-based Query Flow), "
                              + "reason: "
                              + e.getMessage();
                      log.error(errorMessage);
                      failAndSaveBulkOperation(bulkOperation, errorMessage);
                    }
                  }
                }
                case FAILED ->
                    failAndSaveBulkOperation(bulkOperation, queryResult.getFailureReason());
                case CANCELLED -> cancelAndSaveBulkOperation(bulkOperation);
                default ->
                    throw new IllegalStateException("Unexpected value: " + queryResult.getStatus());
              }
            }));
    bulkOperationRepository.save(bulkOperation);
    return bulkOperation;
  }

  protected QueryDetails getQueryResult(BulkOperation bulkOperation) {
    return queryClient.getQuery(bulkOperation.getFqlQueryId(), true);
  }

  public void saveIdentifiers(BulkOperation bulkOperation) {
    try {
      var identifiersString =
          queryClient.getSortedIds(bulkOperation.getFqlQueryId(), 0, Integer.MAX_VALUE).stream()
              .map(List::getFirst)
              .distinct()
              .collect(Collectors.joining(NEW_LINE_SEPARATOR));
      var path = String.format(QUERY_FILENAME_TEMPLATE, bulkOperation.getId());
      remoteFileSystemClient.put(new ByteArrayInputStream(identifiersString.getBytes()), path);
      bulkOperation.setLinkToTriggeringCsvFile(path);
      bulkOperation.setStatus(SAVED_IDENTIFIERS);
      bulkOperation.setApproach(ApproachType.QUERY);
      bulkOperationRepository.save(bulkOperation);
    } catch (Exception e) {
      var errorMessage = "Failed to save identifiers, reason: " + e.getMessage();
      log.error(errorMessage);
      failAndSaveBulkOperation(bulkOperation, errorMessage);
    }
  }

  protected void completeBulkOperation(
      InputStream is,
      BulkOperation operation,
      Set<UUID> uuids,
      List<BulkOperationExecutionContent> bulkOperationExecutionContents) {
    try {
      var triggeringCsvFileName =
          QUERY == operation.getApproach()
              ? String.format(QUERY_FILENAME_TEMPLATE, operation.getId())
              : operation.getLinkToTriggeringCsvFile();
      var matchedJsonFileName =
          getMatchedFileName(operation.getId(), "json/", "Matched", triggeringCsvFileName, "json");
      var matchedMrcFileName =
          getMatchedFileName(
              operation.getId(), StringUtils.EMPTY, "Marc", triggeringCsvFileName, "mrc");
      var matchedCsvFileName =
          getMatchedFileName(
              operation.getId(), StringUtils.EMPTY, "Matched", triggeringCsvFileName, "csv");

      processAsyncQueryResult(
          is,
          triggeringCsvFileName,
          matchedCsvFileName,
          matchedJsonFileName,
          matchedMrcFileName,
          operation,
          uuids,
          bulkOperationExecutionContents);

      if (operation.getMatchedNumOfRecords() > 0) {
        operation.setLinkToTriggeringCsvFile(triggeringCsvFileName);
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
      var linkToMatchingErrorsFile =
          errorService.uploadErrorsToStorage(
              operation.getId(), ERROR_MATCHING_FILE_NAME_PREFIX, e.getMessage());
      operation.setLinkToMatchedRecordsErrorsCsvFile(linkToMatchingErrorsFile);
      bulkOperationRepository.save(operation);
    }
  }

  /**
   * Processes the asynchronous query result.
   *
   * @param is - the input stream of the query result
   * @param triggeringCsvFileName - the name of the triggering CSV file
   * @param matchedCsvFileName - the name of the matched CSV file
   * @param matchedJsonFileName - the name of the matched JSON file
   * @param matchedMrcFileName - the name of the matched MRC file
   * @param operation - the bulk operation
   * @param uuids - the set of UUIDs (WARNING this set is mandatory for the Identifiers Flow and
   *     empty for the Query Flow)
   * @param bulkOperationExecutionContents - the list of bulk operation execution contents
   */
  protected void processAsyncQueryResult(
      InputStream is,
      String triggeringCsvFileName,
      String matchedCsvFileName,
      String matchedJsonFileName,
      String matchedMrcFileName,
      BulkOperation operation,
      Set<UUID> uuids,
      List<BulkOperationExecutionContent> bulkOperationExecutionContents)
      throws IOException, CsvRequiredFieldEmptyException, CsvDataTypeMismatchException {
    Writer writerForTriggeringCsvFile = null;
    try {
      if (ApproachType.QUERY == operation.getApproach()) {
        writerForTriggeringCsvFile = remoteFileSystemClient.writer(triggeringCsvFileName);
      }
      try (var writerForResultCsvFile = remoteFileSystemClient.writer(matchedCsvFileName);
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
        Set<UUID> processedRecordUuids = new HashSet<>();
        while (iterator.hasNext()) {

          var extendedRecord = iterator.next();
          if (extendedRecord.getRecordBulkOperationEntity() instanceof Item item) {
            localReferenceDataService.enrichWithTenant(item, extendedRecord.getTenant());
          }
          if (extendedRecord.getRecordBulkOperationEntity()
              instanceof HoldingsRecord holdingsRecord) {
            localReferenceDataService.enrichWithTenant(holdingsRecord, extendedRecord.getTenant());
          }

          usedTenants.add(extendedRecord.getTenant());

          try {
            permissionsValidator.checkPermissions(operation, extendedRecord);

            if (extendedRecord.isMarcInstance()) {
              processQueryResultForMarc(
                  extendedRecord.getRecordBulkOperationEntity(),
                  writerForResultMrcFile,
                  operation,
                  matchedMrcFileName);
            }

            var extendedRecordAsJsonString = objectMapper.writeValueAsString(extendedRecord);

            writerForResultJsonFile.append(extendedRecordAsJsonString);
            CsvHelper.writeBeanToCsv(
                operation,
                csvWriter,
                extendedRecord.getRecordBulkOperationEntity(),
                bulkOperationExecutionContents);
            numMatched++;
          } catch (UploadFromQueryException e) {
            handleError(
                bulkOperationExecutionContents,
                e,
                extendedRecord.getRecordBulkOperationEntity(),
                operation);
          } finally {
            if (writerForTriggeringCsvFile != null) {
              writerForTriggeringCsvFile.write(
                  extendedRecord.getRecordBulkOperationEntity().getId() + NEW_LINE_SEPARATOR);
            }
          }
          ++numProcessed;

          processedRecordUuids.add(
              fromString(extendedRecord.getRecordBulkOperationEntity().getId()));

          if (numProcessed % STATISTICS_UPDATING_STEP == 0) {
            updateOperationExecutionStatus(operation, numProcessed, numMatched);
          }
        }

        if (CollectionUtils.isNotEmpty(uuids)) {
          SetUtils.difference(uuids, processedRecordUuids)
              .forEach(
                  missingId ->
                      bulkOperationExecutionContents.add(
                          BulkOperationExecutionContent.builder()
                              .identifier(missingId.toString())
                              .bulkOperationId(operation.getId())
                              .state(StateType.FAILED)
                              .errorType(ErrorType.ERROR)
                              .errorMessage(NO_MATCH_FOUND_MESSAGE)
                              .build()));
        }

        operation.setUsedTenants(new ArrayList<>(usedTenants));
        if (numProcessed % STATISTICS_UPDATING_STEP != 0) {
          updateOperationExecutionStatus(operation, numProcessed, numMatched);
        }
        errorService.saveErrorsAfterQuery(bulkOperationExecutionContents, operation);
      }
    } finally {
      if (writerForTriggeringCsvFile != null) {
        writerForTriggeringCsvFile.close();
      }
    }
  }

  private void updateOperationExecutionStatus(
      BulkOperation operation, int numProcessed, int numMatched) {
    operation.setProcessedNumOfRecords(numProcessed);
    operation.setMatchedNumOfRecords(numMatched);
    bulkOperationRepository.save(operation);
  }

  private void failAndSaveBulkOperation(BulkOperation bulkOperation, String errorMessage) {
    bulkOperation.setStatus(FAILED);
    bulkOperation.setErrorMessage(errorMessage);
    bulkOperation.setEndTime(LocalDateTime.now());
    bulkOperationRepository.save(bulkOperation);
  }

  private void cancelAndSaveBulkOperation(BulkOperation bulkOperation) {
    bulkOperation.setStatus(CANCELLED);
    bulkOperation.setErrorMessage("Query execution was cancelled");
    bulkOperation.setEndTime(LocalDateTime.now());
    bulkOperationRepository.save(bulkOperation);
  }

  private void processQueryResultForMarc(
      BulkOperationsEntity entityRecord,
      Writer writerForResultMrcFile,
      BulkOperation operation,
      String matchedMrcFileName)
      throws UploadFromQueryException {
    try {
      var marcJsonString = srsService.getMarcJsonString(entityRecord.getId());
      writerForResultMrcFile.append(marcJsonString);
      if (isNull(operation.getLinkToMatchedRecordsMarcFile())) {
        operation.setLinkToMatchedRecordsMarcFile(matchedMrcFileName);
      }
    } catch (MarcValidationException mve) {
      throw new UploadFromQueryException(mve.getMessage());
    } catch (IOException ioe) {
      throw new UploadFromQueryException(
          NO_MARC_CONTENT.formatted(entityRecord.getId(), ioe.getMessage()), entityRecord.getId());
    }
  }

  private void handleError(
      List<BulkOperationExecutionContent> bulkOperationExecutionContents,
      Exception e,
      BulkOperationsEntity entityRecord,
      BulkOperation operation) {
    log.error(e);
    bulkOperationExecutionContents.add(
        BulkOperationExecutionContent.builder()
            .identifier(entityRecord.getId())
            .bulkOperationId(operation.getId())
            .state(StateType.FAILED)
            .errorType(org.folio.bulkops.domain.dto.ErrorType.ERROR)
            .errorMessage(e.getMessage())
            .build());
  }
}
