package org.folio.bulkops.service;

import static java.lang.String.format;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.folio.bulkops.util.Constants.ERROR_MATCHING_FILE_NAME_PREFIX;
import static org.folio.bulkops.util.Constants.UTC_ZONE;
import static org.folio.bulkops.util.ErrorCode.ERROR_MESSAGE_PATTERN;
import static org.folio.bulkops.util.ErrorCode.ERROR_NOT_DOWNLOAD_ORIGIN_FILE_FROM_S3;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.EnumMap;
import java.util.Map;
import java.util.Spliterators;
import java.util.stream.StreamSupport;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.folio.bulkops.client.RemoteFileSystemClient;
import org.folio.bulkops.domain.bean.BatchStatus;
import org.folio.bulkops.domain.bean.BulkOperationsEntity;
import org.folio.bulkops.domain.bean.Job;
import org.folio.bulkops.domain.bean.JobStatus;
import org.folio.bulkops.domain.bean.Progress;
import org.folio.bulkops.domain.dto.EntityType;
import org.folio.bulkops.domain.dto.OperationStatusType;
import org.folio.bulkops.domain.entity.BulkOperation;
import org.folio.bulkops.exception.ServerErrorException;
import org.folio.bulkops.repository.BulkOperationRepository;
import org.folio.bulkops.util.Utils;
import org.folio.s3.exception.S3ClientException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Log4j2
@RequiredArgsConstructor
public class DataExportJobUpdateService {
  private static final Map<BatchStatus, JobStatus> JOB_STATUSES = new EnumMap<>(BatchStatus.class);
  public static final String FAILED_TO_SAVE_ORIGIN_FILE = "Failed to save origin file";

  static {
    JOB_STATUSES.put(BatchStatus.COMPLETED, JobStatus.SUCCESSFUL);
    JOB_STATUSES.put(BatchStatus.STARTING, JobStatus.IN_PROGRESS);
    JOB_STATUSES.put(BatchStatus.STARTED, JobStatus.IN_PROGRESS);
    JOB_STATUSES.put(BatchStatus.STOPPING, JobStatus.IN_PROGRESS);
    JOB_STATUSES.put(BatchStatus.STOPPED, JobStatus.IN_PROGRESS);
    JOB_STATUSES.put(BatchStatus.FAILED, JobStatus.FAILED);
    JOB_STATUSES.put(BatchStatus.ABANDONED, JobStatus.FAILED);
    JOB_STATUSES.put(BatchStatus.UNKNOWN, null);
  }
  private final BulkOperationRepository bulkOperationRepository;
  private final RemoteFileSystemClient remoteFileSystemClient;
  private final ObjectMapper objectMapper;
  private final ErrorService errorService;

  @Transactional
  public void handleReceivedJobExecutionUpdate(Job jobExecutionUpdate) {
    log.info("Received {}.", jobExecutionUpdate);

    var optionalBulkOperation = bulkOperationRepository.findByDataExportJobId(jobExecutionUpdate.getId());

    if (optionalBulkOperation.isEmpty()) {
      log.error("Update for unknown job {}.", jobExecutionUpdate);
      return;
    }

    var operation = optionalBulkOperation.get();

    var progress = jobExecutionUpdate.getProgress();
    if (nonNull(progress)) {
      operation.setTotalNumOfRecords(isNull(progress.getTotal()) ? 0 : progress.getTotal());
      operation.setProcessedNumOfRecords(isNull(progress.getProcessed()) ? 0 : progress.getProcessed());
    }

    var status = JOB_STATUSES.get(jobExecutionUpdate.getBatchStatus());
    if (nonNull(status)) {
      if (JobStatus.SUCCESSFUL.equals(status)) {
        operation.setStatus(OperationStatusType.SAVING_RECORDS_LOCALLY);
        bulkOperationRepository.save(operation);
        downloadOriginFileAndUpdateBulkOperation(operation, jobExecutionUpdate);
      } else if (JobStatus.FAILED.equals(status)) {
        operation.setStatus(OperationStatusType.FAILED);
        operation.setEndTime(LocalDateTime.ofInstant(ofNullable(jobExecutionUpdate.getEndTime()).orElse(new Date()).toInstant(), UTC_ZONE));
        operation.setErrorMessage(isNull(jobExecutionUpdate.getErrorDetails()) ? EMPTY : jobExecutionUpdate.getErrorDetails());
        var linkToMatchingErrorsFile = errorService.uploadErrorsToStorage(operation.getId(), ERROR_MATCHING_FILE_NAME_PREFIX, operation.getErrorMessage());
        operation.setLinkToMatchedRecordsErrorsCsvFile(linkToMatchingErrorsFile);
      }
    }
    bulkOperationRepository.save(operation);
  }

  private void downloadOriginFileAndUpdateBulkOperation(BulkOperation operation, Job jobUpdate) {
    try {

      operation.setStatus(OperationStatusType.DATA_MODIFICATION);

      var errorsUrl = jobUpdate.getFiles().get(1);
      if (StringUtils.isNotEmpty(errorsUrl)) {
        try (var is = new URL(errorsUrl).openStream()) {
          try (var isSorted = Utils.sortLinesFromInputStream(is)) {
            var linkToMatchingErrorsFile = remoteFileSystemClient.put(isSorted, operation.getId() + "/" + FilenameUtils.getName(errorsUrl.split("\\?")[0]));
            operation.setLinkToMatchedRecordsErrorsCsvFile(linkToMatchingErrorsFile);
          }
        }
      }

      var linkToMatchingRecordsFile = downloadAndSaveCsvFile(operation, jobUpdate);
      var linkToMatchingRecordsMarcFile = downloadAndSaveMarcFile(operation, jobUpdate);
      var linkToOriginFile = downloadAndSaveJsonFile(operation, jobUpdate);

      Progress progress;
      progress = jobUpdate.getProgress();

      operation.setStatus(OperationStatusType.DATA_MODIFICATION);
      operation.setLinkToMatchedRecordsJsonFile(linkToOriginFile);
      operation.setLinkToMatchedRecordsCsvFile(linkToMatchingRecordsFile);
      operation.setLinkToMatchedRecordsMarcFile(linkToMatchingRecordsMarcFile);
      if (nonNull(progress)) {
        operation.setMatchedNumOfRecords(isNull(progress.getSuccess()) ? 0 : progress.getSuccess());
        operation.setMatchedNumOfErrors(isNull(progress.getErrors()) ? 0 : progress.getErrors());
        operation.setMatchedNumOfWarnings(isNull(progress.getWarnings()) ? 0 : progress.getWarnings());
        operation.setTotalNumOfRecords(isNull(progress.getTotal()) ? 0 : progress.getTotal());
        operation.setProcessedNumOfRecords(isNull(progress.getProcessed()) ? 0 : progress.getProcessed());
      }
      operation.setEndTime(LocalDateTime.ofInstant(jobUpdate.getEndTime().toInstant(), UTC_ZONE));

    } catch (S3ClientException e) {
      handleException(operation, jobUpdate, ERROR_NOT_DOWNLOAD_ORIGIN_FILE_FROM_S3, e);
    } catch (Exception e) {
      handleException(operation, jobUpdate, FAILED_TO_SAVE_ORIGIN_FILE, e);
    }
  }

  public String downloadAndSaveJsonFile(BulkOperation bulkOperation, Job jobUpdate) throws IOException {
    var jsonUrl = jobUpdate.getFiles().get(2);
    var path = remoteFileSystemClient.put(new URL(jsonUrl).openStream(), bulkOperation.getId() + "/json/" + FilenameUtils.getName(jsonUrl.split("\\?")[0]));
    if (bulkOperation.getEntityType() == EntityType.ITEM || bulkOperation.getEntityType() == EntityType.HOLDINGS_RECORD) {
      var clazz = Utils.resolveExtendedEntityClass(bulkOperation.getEntityType());
      try (var is = remoteFileSystemClient.get(path)) {
        var parser = objectMapper.createParser(is);
        bulkOperation.setUsedTenants(
          StreamSupport.stream(
              Spliterators.spliteratorUnknownSize(objectMapper.readValues(parser, clazz), 0), false)
            .map(BulkOperationsEntity::getTenant)
            .distinct()
            .toList()
        );
      } catch (Exception e) {
        var error = "Error getting tenants list";
        log.error(error, e);
        bulkOperation.setStatus(OperationStatusType.FAILED);
        bulkOperation.setErrorMessage(error);
        var linkToMatchingErrorsFile = errorService.uploadErrorsToStorage(bulkOperation.getId(), ERROR_MATCHING_FILE_NAME_PREFIX, error + ":" + e.getMessage());
        bulkOperation.setLinkToMatchedRecordsErrorsCsvFile(linkToMatchingErrorsFile);
        throw new ServerErrorException("Error getting tenants list: " + e.getMessage());
      } finally {
        bulkOperationRepository.save(bulkOperation);
      }
    }
    return path;
  }

  public String downloadAndSaveCsvFile(BulkOperation bulkOperation, Job jobUpdate) throws IOException {
    var csvUrl = jobUpdate.getFiles().get(0);
    return isEmpty(csvUrl) ?
      null :
      remoteFileSystemClient.put(new URL(csvUrl).openStream(), bulkOperation.getId() + "/" + FilenameUtils.getName(csvUrl.split("\\?")[0]));
  }

  public String downloadAndSaveMarcFile(BulkOperation bulkOperation, Job jobUpdate) throws IOException {
    var marcUrl = jobUpdate.getFiles().get(3);
    return isEmpty(marcUrl) ?
      null :
      remoteFileSystemClient.put(new URL(marcUrl).openStream(), bulkOperation.getId() + "/" + FilenameUtils.getName(marcUrl.split("\\?")[0]));
  }

  private void handleException(BulkOperation operation, Job jobUpdate, String message, Exception e) {
    log.error(message, e);
    operation.setErrorMessage(format(ERROR_MESSAGE_PATTERN, message, e.getMessage()));
    operation.setStatus(OperationStatusType.COMPLETED_WITH_ERRORS);
    operation.setEndTime(LocalDateTime.now());
    if (ObjectUtils.isNotEmpty(jobUpdate.getProgress())) {
      operation.setMatchedNumOfErrors(isNull(jobUpdate.getProgress().getErrors()) ? 0 : jobUpdate.getProgress().getErrors());
      operation.setMatchedNumOfWarnings(isNull(jobUpdate.getProgress().getWarnings()) ? 0 : jobUpdate.getProgress().getWarnings());
    }
  }
}
