package org.folio.bulkops.batch;

import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.folio.bulkops.domain.bean.JobParameterNames.BULK_OPERATION_ID;
import static org.folio.bulkops.domain.bean.JobParameterNames.STORAGE_FILE_PATH;
import static org.folio.bulkops.domain.bean.JobParameterNames.TEMP_LOCAL_MARC_PATH;
import static org.folio.bulkops.domain.bean.JobParameterNames.TEMP_LOCAL_FILE_PATH;
import static org.folio.bulkops.domain.bean.JobParameterNames.STORAGE_MARC_PATH;
import static org.folio.bulkops.util.Constants.ERROR_MATCHING_FILE_NAME_PREFIX;
import static org.folio.bulkops.util.Constants.IDENTIFIERS_FILE_NAME;
import static org.folio.bulkops.util.Constants.NUMBER_OF_MATCHED_RECORDS;
import static org.folio.bulkops.util.Constants.NUMBER_OF_PROCESSED_IDENTIFIERS;
import static org.springframework.batch.core.BatchStatus.ABANDONED;
import static org.springframework.batch.core.BatchStatus.COMPLETED;
import static org.springframework.batch.core.BatchStatus.FAILED;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.folio.bulkops.client.RemoteFileSystemClient;
import org.folio.bulkops.domain.dto.OperationStatusType;
import org.folio.bulkops.domain.entity.BulkOperation;
import org.folio.bulkops.repository.BulkOperationRepository;
import org.folio.bulkops.service.ErrorService;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.JobParameters;
import org.springframework.stereotype.Component;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@Log4j2
@RequiredArgsConstructor
public class JobCompletionNotificationListener implements JobExecutionListener {
  private final RemoteFileSystemClient remoteFileSystemClient;
  private final BulkOperationRepository bulkOperationRepository;
  private final ErrorService errorService;

  @Override
  public void beforeJob(JobExecution jobExecution) {
    processJobUpdate(jobExecution, false);
  }

  @Override
  public void afterJob(JobExecution jobExecution) {
    processJobUpdate(jobExecution, true);
  }

  @SneakyThrows
  private void processJobUpdate(JobExecution jobExecution, boolean after) {
    ofNullable(jobExecution.getJobParameters().getString(BULK_OPERATION_ID))
      .map(UUID::fromString)
      .map(bulkOperationRepository::findById)
      .flatMap(opt -> opt)
      .ifPresent(bulkOperation -> {
        if (after) {
          var jobParameters = jobExecution.getJobParameters();
          moveTemporaryFilesToStorage(jobParameters, bulkOperation);
          handleProcessingMatchedErrors(bulkOperation);
          log.info("-----------------------------JOB---ENDS-----------------------------");
        }
        var context = jobExecution.getExecutionContext();
        bulkOperation.setProcessedNumOfRecords(context.containsKey(NUMBER_OF_PROCESSED_IDENTIFIERS) ? context.getInt(NUMBER_OF_PROCESSED_IDENTIFIERS) : 0);
        bulkOperation.setMatchedNumOfRecords(context.containsKey(NUMBER_OF_MATCHED_RECORDS) ? context.getInt(NUMBER_OF_MATCHED_RECORDS) : 0);
        if (COMPLETED.equals(jobExecution.getStatus())) {
          bulkOperation.setStatus(OperationStatusType.DATA_MODIFICATION);
        } else if (Set.of(FAILED, ABANDONED).contains(jobExecution.getStatus())) {
          bulkOperation.setStatus(OperationStatusType.FAILED);
          bulkOperation.setErrorMessage(fetchFailureCause(jobExecution));
          bulkOperation.setEndTime(LocalDateTime.now());
        }
        bulkOperationRepository.save(bulkOperation);
      });
  }

  private String fetchFailureCause(JobExecution jobExecution) {
    List<Throwable> errors = jobExecution.getAllFailureExceptions();
    if (CollectionUtils.isNotEmpty(errors)) {
      return errors.stream()
        .map(t -> {
          t = getThrowableRootCause(t);
          return t.getMessage() + " (" + t.getClass().getSimpleName() + ')';
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

  private void moveTemporaryFilesToStorage(JobParameters jobParameters, BulkOperation bulkOperation) {
    try {
      var tmpFileName = jobParameters.getString(TEMP_LOCAL_FILE_PATH);
      if (nonNull(tmpFileName)) {
        var csvFileName = jobParameters.getString(STORAGE_FILE_PATH) + ".csv";
        moveFileToStorage(csvFileName, tmpFileName);
        bulkOperation.setLinkToMatchedRecordsCsvFile(csvFileName);
        var jsonFileName = jobParameters.getString(STORAGE_FILE_PATH) + ".json";
        moveFileToStorage(jsonFileName, tmpFileName + ".json");
        bulkOperation.setLinkToMatchedRecordsJsonFile(jsonFileName);
      }
      var tmpMarcName = jobParameters.getString(TEMP_LOCAL_MARC_PATH) + ".mrc";
      var marcFileName = jobParameters.getString(STORAGE_MARC_PATH) + ".mrc";
      if (Files.exists(Path.of(tmpMarcName))) {
        moveFileToStorage(marcFileName, tmpMarcName);
        bulkOperation.setLinkToMatchedRecordsMarcFile(marcFileName);
      }

      var tmpIdentifiersFileName = jobParameters.getString(IDENTIFIERS_FILE_NAME);
      if (nonNull(tmpIdentifiersFileName) && Files.deleteIfExists(Path.of(tmpIdentifiersFileName))) {
        log.info("Deleted temporary identifiers file: {}", tmpIdentifiersFileName);
      }
    } catch (IOException e) {
      log.error("Failed to move temporary files", e);
    }
  }

  private void moveFileToStorage(String destFileName, String sourceFileName) throws IOException {
    var sourcePath = Path.of(sourceFileName);
    if (Files.exists(sourcePath)) {
      remoteFileSystemClient.put(new FileInputStream(sourceFileName), destFileName);
      if (Files.deleteIfExists(sourcePath)) {
        log.info("Deleted temporary file: {}", sourceFileName);
      }
    }
  }

  private void handleProcessingMatchedErrors(BulkOperation bulkOperation) {
    var pathToErrors = errorService.uploadErrorsToStorage(bulkOperation.getId(), ERROR_MATCHING_FILE_NAME_PREFIX, null);
    bulkOperation.setLinkToMatchedRecordsErrorsCsvFile(pathToErrors);
    bulkOperation.setMatchedNumOfErrors(errorService.getCommittedNumOfErrors(bulkOperation.getId()));
    bulkOperation.setMatchedNumOfWarnings(errorService.getCommittedNumOfWarnings(bulkOperation.getId()));
  }
}
