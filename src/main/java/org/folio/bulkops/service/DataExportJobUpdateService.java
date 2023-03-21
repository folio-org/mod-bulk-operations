package org.folio.bulkops.service;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.folio.bulkops.domain.dto.ApproachType.QUERY;
import static org.folio.bulkops.util.Constants.UTC_ZONE;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.ObjectUtils;
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
import org.folio.spring.DefaultFolioExecutionContext;
import org.folio.spring.FolioModuleMetadata;
import org.folio.spring.integration.XOkapiHeaders;
import org.folio.spring.scope.FolioExecutionContextSetter;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

  private final FolioModuleMetadata folioModuleMetadata;
  private final BulkOperationRepository bulkOperationRepository;
  private final RemoteFileSystemClient remoteFileSystemClient;
  private final ObjectMapper objectMapper;

  @Transactional
  @KafkaListener(
      id = KafkaService.EVENT_LISTENER_ID,
      containerFactory = "kafkaListenerContainerFactory",
      topicPattern = "${application.kafka.topic-pattern}",
      groupId = "${application.kafka.group-id}")
  public void receiveJobExecutionUpdate(@Payload Job jobExecutionUpdate, @Headers Map<String, Object> messageHeaders) {
    var okapiHeaders =
      messageHeaders.entrySet()
        .stream()
        .filter(e -> e.getKey().startsWith(XOkapiHeaders.OKAPI_HEADERS_PREFIX))
        .collect(Collectors.toMap(Map.Entry::getKey, e -> (Collection<String>)List.of(String.valueOf(e.getValue()))));

    try (var context =  new FolioExecutionContextSetter(new DefaultFolioExecutionContext(folioModuleMetadata, okapiHeaders))) {
      log.info("Received {}.", jobExecutionUpdate);

      var optionalBulkOperation = bulkOperationRepository.findByDataExportJobId(jobExecutionUpdate.getId());

      if (optionalBulkOperation.isEmpty()) {
        log.error("Update for unknown job {}.", jobExecutionUpdate);
        return;
      }

      var operation = optionalBulkOperation.get();

      if (nonNull(jobExecutionUpdate.getProgress())) {
        operation.setTotalNumOfRecords(jobExecutionUpdate.getProgress().getTotal());
        operation.setProcessedNumOfRecords(jobExecutionUpdate.getProgress().getProcessed());
      }

      var status = JOB_STATUSES.get(jobExecutionUpdate.getBatchStatus());
      if (nonNull(status)) {
        if (JobStatus.SUCCESSFUL.equals(status)) {
          operation.setStatus(OperationStatusType.SAVING_RECORDS_LOCALLY);
          bulkOperationRepository.save(operation);
          downloadOriginFileAndUpdateBulkOperation(operation, jobExecutionUpdate);
        } else if (JobStatus.FAILED.equals(status)) {
          operation.setStatus(OperationStatusType.FAILED);
          operation.setEndTime(LocalDateTime.ofInstant(jobExecutionUpdate.getEndTime().toInstant(), UTC_ZONE));
          operation.setErrorMessage(isNull(jobExecutionUpdate.getErrorDetails()) ? EMPTY : jobExecutionUpdate.getErrorDetails());
        }
      }
      bulkOperationRepository.save(operation);
    }
  }

  private void downloadOriginFileAndUpdateBulkOperation(BulkOperation operation, Job jobUpdate) {
    try {

      operation.setStatus(OperationStatusType.DATA_MODIFICATION);

      var errorsUrl = jobUpdate.getFiles().get(1);
      if (StringUtils.isNotEmpty(errorsUrl)) {
        try(var is = new URL(errorsUrl).openStream()) {
          var linkToMatchingErrorsFile = remoteFileSystemClient.put(is, operation.getId() + "/" + FilenameUtils.getName(errorsUrl.split("\\?")[0]));
          operation.setLinkToMatchedRecordsErrorsCsvFile(linkToMatchingErrorsFile);
        }
      }

      var linkToMatchingRecordsFile = downloadAndSaveCsvFile(operation, jobUpdate);
      var linkToOriginFile = downloadAndSaveJsonFile(operation, jobUpdate);

      Progress progress;
      if (QUERY == operation.getApproach()) {
        var value = remoteFileSystemClient.getNumOfLines(linkToMatchingRecordsFile) - 1;
        progress =  new Progress()
          .withErrors(0)
          .withSuccess(value)
          .withTotal(value)
          .withProcessed(value);
      } else {
        progress = jobUpdate.getProgress();
      }

      operation.setStatus(OperationStatusType.DATA_MODIFICATION);
      operation.setLinkToMatchedRecordsJsonFile(linkToOriginFile);
      operation.setLinkToMatchedRecordsCsvFile(linkToMatchingRecordsFile);
      operation.setMatchedNumOfRecords(progress.getSuccess());
      operation.setMatchedNumOfErrors(progress.getErrors());
      operation.setTotalNumOfRecords(progress.getTotal());
      operation.setProcessedNumOfRecords(progress.getProcessed());
      operation.setEndTime(LocalDateTime.ofInstant(jobUpdate.getEndTime().toInstant(), UTC_ZONE));

    } catch (Exception e) {
      var msg = "Failed to download origin file, reason: " + e;
      log.error(msg);
      operation.setStatus(OperationStatusType.COMPLETED_WITH_ERRORS);
      operation.setEndTime(LocalDateTime.now());
      if (ObjectUtils.isNotEmpty(jobUpdate.getProgress())) {
        operation.setMatchedNumOfErrors(jobUpdate.getProgress().getErrors());
      }
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
