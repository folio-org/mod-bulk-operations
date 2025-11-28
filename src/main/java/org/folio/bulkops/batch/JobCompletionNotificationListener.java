package org.folio.bulkops.batch;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.folio.bulkops.domain.bean.JobParameterNames.BULK_OPERATION_ID;
import static org.folio.bulkops.domain.bean.JobParameterNames.STORAGE_FILE_PATH;
import static org.folio.bulkops.domain.bean.JobParameterNames.STORAGE_MARC_PATH;
import static org.folio.bulkops.domain.bean.JobParameterNames.TEMP_LOCAL_FILE_PATH;
import static org.folio.bulkops.domain.bean.JobParameterNames.TEMP_LOCAL_MARC_PATH;
import static org.folio.bulkops.util.Constants.ERROR_MATCHING_FILE_NAME_PREFIX;
import static org.folio.bulkops.util.Constants.IDENTIFIERS_FILE_NAME;
import static org.folio.bulkops.util.Constants.NUMBER_OF_MATCHED_RECORDS;
import static org.folio.bulkops.util.Constants.NUMBER_OF_PROCESSED_IDENTIFIERS;
import static org.springframework.batch.core.BatchStatus.ABANDONED;
import static org.springframework.batch.core.BatchStatus.COMPLETED;
import static org.springframework.batch.core.BatchStatus.FAILED;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.Spliterators;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.folio.bulkops.client.RemoteFileSystemClient;
import org.folio.bulkops.domain.bean.BulkOperationsEntity;
import org.folio.bulkops.domain.dto.OperationStatusType;
import org.folio.bulkops.domain.entity.BulkOperation;
import org.folio.bulkops.exception.ServerErrorException;
import org.folio.bulkops.repository.BulkOperationRepository;
import org.folio.bulkops.service.ErrorService;
import org.folio.bulkops.util.Utils;
import org.jetbrains.annotations.NotNull;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.JobParameters;
import org.springframework.stereotype.Component;

@Component
@Log4j2
@RequiredArgsConstructor
public class JobCompletionNotificationListener implements JobExecutionListener {
  private final RemoteFileSystemClient remoteFileSystemClient;
  private final BulkOperationRepository bulkOperationRepository;
  private final ErrorService errorService;
  private final ObjectMapper objectMapper;

  @Override
  public void beforeJob(@NotNull JobExecution jobExecution) {
    processJobUpdate(jobExecution, false);
  }

  @Override
  public void afterJob(@NotNull JobExecution jobExecution) {
    processJobUpdate(jobExecution, true);
  }

  @SneakyThrows
  private void processJobUpdate(JobExecution jobExecution, boolean after) {
    var bulkOpId = jobExecution.getJobParameters().getString(BULK_OPERATION_ID);

    ofNullable(bulkOpId)
        .map(UUID::fromString)
        .map(bulkOperationRepository::findById)
        .flatMap(opt -> opt)
        .ifPresent(
            bulkOperation -> {
              if (after) {
                var jobParameters = jobExecution.getJobParameters();
                moveTemporaryFilesToStorage(jobParameters, bulkOperation);
                handleProcessingMatchedErrors(bulkOperation);
                if (nonNull(bulkOperation.getLinkToMatchedRecordsJsonFile())) {
                  populateUsedTenants(bulkOperation);
                }

                log.info("-----------------------------JOB---ENDS-----------------------------");
              }

              var context = jobExecution.getExecutionContext();

              int processed =
                  context.containsKey(NUMBER_OF_PROCESSED_IDENTIFIERS)
                      ? context.getInt(NUMBER_OF_PROCESSED_IDENTIFIERS)
                      : 0;
              bulkOperation.setProcessedNumOfRecords(processed);

              int matched =
                  context.containsKey(NUMBER_OF_MATCHED_RECORDS)
                      ? context.getInt(NUMBER_OF_MATCHED_RECORDS)
                      : 0;
              bulkOperation.setMatchedNumOfRecords(matched);

              if (COMPLETED.equals(jobExecution.getStatus())) {
                bulkOperation.setStatus(OperationStatusType.DATA_MODIFICATION);
                bulkOperation.setEndTime(LocalDateTime.now());

                if (nonNull(bulkOperation.getLinkToMatchedRecordsErrorsCsvFile())
                    && isNull(bulkOperation.getLinkToMatchedRecordsCsvFile())) {
                  bulkOperation.setStatus(OperationStatusType.COMPLETED_WITH_ERRORS);
                }
              } else if (Set.of(FAILED, ABANDONED).contains(jobExecution.getStatus())) {
                bulkOperation.setStatus(OperationStatusType.FAILED);
                bulkOperation.setErrorMessage(fetchFailureCause(jobExecution));
                bulkOperation.setEndTime(LocalDateTime.now());
              }

              bulkOperationRepository.save(bulkOperation);
            });
  }

  private void populateUsedTenants(BulkOperation bulkOperation) {
    if (bulkOperation.getEntityType() == org.folio.bulkops.domain.dto.EntityType.ITEM
        || bulkOperation.getEntityType()
            == org.folio.bulkops.domain.dto.EntityType.HOLDINGS_RECORD) {
      var clazz = Utils.resolveExtendedEntityClass(bulkOperation.getEntityType());
      try (var is = remoteFileSystemClient.get(bulkOperation.getLinkToMatchedRecordsJsonFile())) {
        var parser = objectMapper.createParser(is);
        var iterator = objectMapper.readValues(parser, clazz);
        var spliterator = Spliterators.spliteratorUnknownSize(iterator, 0);
        var tenants =
            StreamSupport.stream(spliterator, false)
                .map(BulkOperationsEntity::getTenant)
                .distinct()
                .toList();
        bulkOperation.setUsedTenants(tenants);
      } catch (Exception e) {
        var error = "Error getting tenants list";
        log.error(error, e);
        bulkOperation.setStatus(OperationStatusType.FAILED);
        bulkOperation.setErrorMessage(error);
        var errorMessage = String.format("%s: %s", error, e.getMessage());
        var linkToMatchingErrorsFile =
            errorService.uploadErrorsToStorage(
                bulkOperation.getId(), ERROR_MATCHING_FILE_NAME_PREFIX, errorMessage);
        bulkOperation.setLinkToMatchedRecordsErrorsCsvFile(linkToMatchingErrorsFile);
        var serverErrorMessage = "Error getting tenants list: " + e.getMessage();
        throw new ServerErrorException(serverErrorMessage);
      } finally {
        bulkOperationRepository.save(bulkOperation);
      }
    }
  }

  private String fetchFailureCause(JobExecution jobExecution) {
    List<Throwable> errors = jobExecution.getAllFailureExceptions();
    if (CollectionUtils.isNotEmpty(errors)) {
      return errors.stream()
          .map(
              t -> {
                var root = getThrowableRootCause(t);
                var className = root.getClass().getSimpleName();
                return String.format("%s (%s)", root.getMessage(), className);
              })
          .collect(Collectors.joining("\n"));
    }
    return EMPTY;
  }

  private Throwable getThrowableRootCause(Throwable t) {
    Throwable cause = t.getCause();
    while (cause != null && cause != t) {
      t = cause;
      cause = t.getCause();
    }
    return t;
  }

  private void moveTemporaryFilesToStorage(
      JobParameters jobParameters, BulkOperation bulkOperation) {
    try {
      var tmpFileName = jobParameters.getString(TEMP_LOCAL_FILE_PATH);
      log.info("Moving temporary file: {}", tmpFileName);
      if (nonNull(tmpFileName)) {
        var path = Path.of(tmpFileName);
        if (Files.exists(path) && Files.size(path) > 0) {
          var storageFileBase = jobParameters.getString(STORAGE_FILE_PATH);
          var csvFileName = storageFileBase + ".csv";
          moveFileToStorage(csvFileName, tmpFileName);
          bulkOperation.setLinkToMatchedRecordsCsvFile(csvFileName);

          var jsonFileName = storageFileBase + ".json";
          moveFileToStorage(jsonFileName, tmpFileName + ".json");
          bulkOperation.setLinkToMatchedRecordsJsonFile(jsonFileName);
        }
      }

      var tmpMarcBase = jobParameters.getString(TEMP_LOCAL_MARC_PATH);
      var tmpMarcName = tmpMarcBase + ".mrc";
      var marcFileBase = jobParameters.getString(STORAGE_MARC_PATH);
      var marcFileName = marcFileBase + ".mrc";

      if (Files.exists(Path.of(tmpMarcName))) {
        moveFileToStorage(marcFileName, tmpMarcName);
        bulkOperation.setLinkToMatchedRecordsMarcFile(marcFileName);
      }

      var tmpIdentifiersFileName = jobParameters.getString(IDENTIFIERS_FILE_NAME);
      boolean identifiersDeleted = false;
      if (nonNull(tmpIdentifiersFileName)) {
        identifiersDeleted = Files.deleteIfExists(Path.of(tmpIdentifiersFileName));
      }

      if (identifiersDeleted) {
        log.info("Deleted temporary identifiers file: {}", tmpIdentifiersFileName);
      }
    } catch (IOException e) {
      log.error("Failed to move temporary files", e);
    }
  }

  private void moveFileToStorage(String destFileName, String sourceFileName) throws IOException {
    var sourcePath = Path.of(sourceFileName);
    if (Files.exists(sourcePath)) {
      try (var fis = new FileInputStream(sourceFileName)) {
        remoteFileSystemClient.put(fis, destFileName);
      }
      if (Files.deleteIfExists(sourcePath)) {
        log.info("Deleted temporary file: {}", sourceFileName);
      }
    }
  }

  private void handleProcessingMatchedErrors(BulkOperation bulkOperation) {
    var pathToErrors =
        errorService.uploadErrorsToStorage(
            bulkOperation.getId(), ERROR_MATCHING_FILE_NAME_PREFIX, null);
    bulkOperation.setLinkToMatchedRecordsErrorsCsvFile(pathToErrors);
    bulkOperation.setMatchedNumOfErrors(
        errorService.getCommittedNumOfErrors(bulkOperation.getId()));
    bulkOperation.setMatchedNumOfWarnings(
        errorService.getCommittedNumOfWarnings(bulkOperation.getId()));
  }
}
