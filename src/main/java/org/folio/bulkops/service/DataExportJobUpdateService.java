package org.folio.bulkops.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.folio.bulkops.client.RemoteFileSystemClient;
import org.folio.bulkops.configs.kafka.KafkaService;
import org.folio.bulkops.domain.bean.BatchStatus;
import org.folio.bulkops.domain.bean.Job;
import org.folio.bulkops.domain.bean.JobStatus;
import org.folio.bulkops.domain.bean.Progress;
import org.folio.bulkops.domain.dto.OperationStatusType;
import org.folio.bulkops.domain.entity.BulkOperation;
import org.folio.bulkops.repository.BulkOperationRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.Map;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.folio.bulkops.domain.dto.ApproachType.QUERY;
import static org.folio.bulkops.util.Constants.UTC_ZONE;

@Service
@Log4j2
@RequiredArgsConstructor
public class DataExportJobUpdateService {
  private static final Map<BatchStatus, JobStatus> JOB_STATUSES = new EnumMap<>(BatchStatus.class);

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

  @Transactional
  @KafkaListener(
      id = KafkaService.EVENT_LISTENER_ID,
      containerFactory = "kafkaListenerContainerFactory",
      topicPattern = "${application.kafka.topic-pattern}",
      groupId = "${application.kafka.group-id}")
  public void receiveJobExecutionUpdate(Job jobExecutionUpdate) {
    log.info("Received {}.", jobExecutionUpdate);

    var optionalBulkOperation = bulkOperationRepository.findByDataExportJobId(jobExecutionUpdate.getId());

    if (optionalBulkOperation.isEmpty()) {
      log.error("Update for unknown job {}.", jobExecutionUpdate.getId());
      return;
    }

    var bulkOperation = optionalBulkOperation.get();

    if (nonNull(jobExecutionUpdate.getProgress())) {
      bulkOperation = bulkOperation
        .withTotalNumOfRecords(jobExecutionUpdate.getProgress().getTotal())
        .withProcessedNumOfRecords(jobExecutionUpdate.getProgress().getProcessed());
    }

    var status = JOB_STATUSES.get(jobExecutionUpdate.getBatchStatus());
    if (nonNull(status)) {
      if (JobStatus.SUCCESSFUL.equals(status)) {
        bulkOperationRepository.save(bulkOperation.withStatus(OperationStatusType.SAVING_RECORDS_LOCALLY));
        bulkOperation = downloadOriginFileAndUpdateBulkOperation(bulkOperation, jobExecutionUpdate);
      } else if (JobStatus.FAILED.equals(status)) {
        bulkOperation = bulkOperation
          .withStatus(OperationStatusType.FAILED)
          .withEndTime(LocalDateTime.ofInstant(jobExecutionUpdate.getEndTime().toInstant(), UTC_ZONE))
          .withErrorMessage(isNull(jobExecutionUpdate.getErrorDetails()) ? EMPTY : jobExecutionUpdate.getErrorDetails());
      }
    }

    bulkOperationRepository.save(bulkOperation);
  }

  private BulkOperation downloadOriginFileAndUpdateBulkOperation(BulkOperation bulkOperation, Job jobUpdate) {
    try {

      bulkOperation.setStatus(OperationStatusType.DATA_MODIFICATION);

      var errorsUrl = jobUpdate.getFiles().get(1);
      if (StringUtils.isNotEmpty(errorsUrl)) {
        try(var is = new URL(errorsUrl).openStream()) {
          var linkToMatchingErrorsFile = remoteFileSystemClient.put(is, bulkOperation.getId() + "/" + FilenameUtils.getName(errorsUrl.split("\\?")[0]));
          bulkOperation.setLinkToMatchedRecordsErrorsCsvFile(linkToMatchingErrorsFile);
        }
      }

      var linkToMatchingRecordsFile = downloadAndSaveCsvFile(bulkOperation, jobUpdate);
      var linkToOriginFile = downloadAndSaveJsonFile(bulkOperation, jobUpdate);

      Progress progress;
      if (QUERY == bulkOperation.getApproach()) {
        var value = remoteFileSystemClient.getNumOfLines(linkToMatchingRecordsFile);
        progress =  new Progress()
          .withErrors(0)
          .withSuccess(value)
          .withTotal(value)
          .withProcessed(value);
      } else {
        progress = jobUpdate.getProgress();
      }

      return bulkOperation
        .withStatus(OperationStatusType.DATA_MODIFICATION)
        .withLinkToMatchedRecordsJsonFile(linkToOriginFile)
        .withLinkToMatchedRecordsCsvFile(linkToMatchingRecordsFile)
        .withMatchedNumOfRecords(progress.getSuccess())
        .withMatchedNumOfErrors(progress.getErrors())
        .withTotalNumOfRecords(progress.getTotal())
        .withProcessedNumOfRecords(progress.getProcessed())
        .withEndTime(LocalDateTime.ofInstant(jobUpdate.getEndTime().toInstant(), UTC_ZONE));
    } catch (Exception e) {
      var msg = "Failed to download origin file, reason: " + e;
      log.error(msg);
      return bulkOperation
        .withStatus(OperationStatusType.COMPLETED_WITH_ERRORS)
        .withMatchedNumOfErrors(jobUpdate.getProgress().getErrors())
        .withEndTime(LocalDateTime.now());
    }
  }

  public String downloadAndSaveJsonFile(BulkOperation bulkOperation, Job jobUpdate) throws IOException {
    var jsonUrl = jobUpdate.getFiles().get(2);
    return remoteFileSystemClient.put(new URL(jsonUrl).openStream(), bulkOperation.getId() + "/json/" + FilenameUtils.getName(jsonUrl.split("\\?")[0]));
  }

  public String downloadAndSaveCsvFile(BulkOperation bulkOperation, Job jobUpdate) throws IOException {
    var csvUrl = jobUpdate.getFiles().get(0);
    return remoteFileSystemClient.put(new URL(csvUrl).openStream(), bulkOperation.getId() + "/" + FilenameUtils.getName(csvUrl.split("\\?")[0]));
  }
}
