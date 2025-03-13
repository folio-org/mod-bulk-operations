package org.folio.bulkops.service;

import static java.lang.Boolean.TRUE;
import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.folio.bulkops.domain.bean.JobLogEntry.ActionStatus.UPDATED;
import static org.folio.bulkops.domain.dto.DataImportStatus.CANCELLED;
import static org.folio.bulkops.domain.dto.DataImportStatus.COMMITTED;
import static org.folio.bulkops.domain.dto.DataImportStatus.ERROR;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.bulkops.client.DataImportClient;
import org.folio.bulkops.client.MetadataProviderClient;
import org.folio.bulkops.domain.bean.JobLogEntry;
import org.folio.bulkops.domain.bean.RelatedInstanceInfo;
import org.folio.bulkops.domain.dto.DataImportJobExecution;
import org.folio.bulkops.domain.dto.DataImportProgress;
import org.folio.bulkops.domain.dto.DataImportStatus;
import org.folio.bulkops.domain.dto.ErrorType;
import org.folio.bulkops.domain.entity.BulkOperation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;

@Service
@RequiredArgsConstructor
@Log4j2
public class MetadataProviderService {
  public static final String MSG_FAILED_TO_GET_LOG_ENTRIES_CHUNK = "Failed to get %d log entries, reason: %s";
  private static final int GET_JOB_LOG_ENTRIES_MAX_RETRIES = 10;
  private static final int GET_JOB_LOG_ENTRIES_RETRY_TIMEOUT = 5000;

  private final Set<DataImportStatus> completedStatuses = Set.of(COMMITTED, CANCELLED, ERROR);
  private final MetadataProviderClient metadataProviderClient;
  private final DataImportClient dataImportClient;
  private final ErrorService errorService;

  @Value("${application.data-import-integration.num_of_concurrent_requests}")
  private int numOfConcurrentRequests;
  @Value("${application.data-import-integration.chunk_size}")
  private int chunkSize;

  public List<DataImportJobExecution> getJobExecutions(UUID jobProfileId) {
    var splitStatus = dataImportClient.getSplitStatus();
    var subordinationType = TRUE.equals(splitStatus.getSplitStatus()) ?
      DataImportJobExecution.SubordinationTypeEnum.COMPOSITE_CHILD :
      DataImportJobExecution.SubordinationTypeEnum.PARENT_SINGLE;
    return metadataProviderClient.getJobExecutionsByJobProfileId(jobProfileId, Integer.MAX_VALUE).getJobExecutions().stream()
      .filter(jobExecution -> nonNull(jobExecution.getJobProfileInfo()))
      .filter(jobExecution -> jobExecution.getJobProfileInfo().getId().equals(jobProfileId))
      .filter(jobExecution -> subordinationType.equals(jobExecution.getSubordinationType()))
      .toList();
  }

  public boolean isDataImportJobCompleted(List<DataImportJobExecution> jobExecutions) {
    return !jobExecutions.isEmpty() && jobExecutions.size() == jobExecutions.get(0).getTotalJobParts() && jobExecutions.stream()
      .allMatch(jobExecution -> completedStatuses.contains(jobExecution.getStatus()));
  }

  public DataImportProgress calculateProgress(List<DataImportJobExecution> jobExecutions) {
    var progress = new DataImportProgress().current(0).total(0);
    jobExecutions.stream()
      .map(DataImportJobExecution::getProgress)
      .forEach(dataImportProgress -> {
        progress.setCurrent(progress.getCurrent() + dataImportProgress.getCurrent());
        progress.setTotal(progress.getTotal() + dataImportProgress.getTotal());
      });
    return progress;
  }

  public List<String> fetchUpdatedInstanceIds(List<JobLogEntry> logEntries) {
    return logEntries.stream()
      .filter(entry -> UPDATED.equals(entry.getSourceRecordActionStatus()))
      .map(JobLogEntry::getRelatedInstanceInfo)
      .map(RelatedInstanceInfo::getIdList)
      .flatMap(List::stream)
      .toList();
  }

  public List<JobLogEntry> getJobLogEntries(BulkOperation bulkOperation, List<DataImportJobExecution> jobExecutions) {
    return jobExecutions.stream()
      .map(execution -> getExecutionLogEntries(bulkOperation, execution))
      .flatMap(List::stream)
      .toList();
  }

  private List<JobLogEntry> getExecutionLogEntries(BulkOperation bulkOperation, DataImportJobExecution jobExecution) {
    try (var fjPool = new ForkJoinPool(numOfConcurrentRequests)) {
      var jobExecutionId = jobExecution.getId().toString();
      var retryCounter = 0;
      var totalJobLogEntries = 0;
      var expectedJobLogEntriesNumber = jobExecution.getProgress().getTotal();
      while (retryCounter < GET_JOB_LOG_ENTRIES_MAX_RETRIES) {
        totalJobLogEntries = metadataProviderClient.getJobLogEntries(jobExecutionId, 1).getTotalRecords();
        log.info("Get log entries attempt #{}, total job log entries: {}, expected value: {}", retryCounter, totalJobLogEntries, expectedJobLogEntriesNumber);
        if (totalJobLogEntries == expectedJobLogEntriesNumber) {
          break;
        } else {
          if (++retryCounter == GET_JOB_LOG_ENTRIES_MAX_RETRIES) {
            log.error("Expected number of job log entries ({}) was not reached after {} retries", expectedJobLogEntriesNumber, retryCounter);
          } else {
            Thread.sleep(GET_JOB_LOG_ENTRIES_RETRY_TIMEOUT);
          }
        }
      }
      var offsets = splitOffsets(expectedJobLogEntriesNumber);
      return fjPool.submit(() -> offsets.stream().parallel()
        .map(offset -> getEntries(bulkOperation, jobExecutionId, offset, chunkSize))
        .flatMap(List::stream)
        .toList()).get();
    } catch (ExecutionException | InterruptedException e) {
      log.error("Failed to retrieve job log entries", e);
      Thread.currentThread().interrupt();
      return Collections.emptyList();
    }
  }

  private List<Integer> splitOffsets(int totalRecords) {
    List<Integer> offsets = new ArrayList<>();
    for (int i = 0; i < totalRecords; i += chunkSize) {
      offsets.add(i);
    }
    return offsets;
  }

  private List<JobLogEntry> getEntries(BulkOperation bulkOperation, String jobExecutionId, int offset, int limit) {
    try {
      return metadataProviderClient.getJobLogEntries(jobExecutionId, offset, limit).getEntries();
    } catch (Exception e) {
      log.error("Failed to get chunk offset={}, limit={}, reason: {}", offset, limit, e.getMessage());
      errorService.saveError(bulkOperation.getId(), EMPTY, MSG_FAILED_TO_GET_LOG_ENTRIES_CHUNK.formatted(limit, e.getMessage()), ErrorType.ERROR);
      return Collections.emptyList();
    }
  }
}
