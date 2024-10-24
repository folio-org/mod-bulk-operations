package org.folio.bulkops.service;

import static java.lang.Boolean.TRUE;
import static java.util.Objects.nonNull;
import static org.folio.bulkops.domain.bean.JobLogEntry.ActionStatus.UPDATED;
import static org.folio.bulkops.domain.dto.DataImportStatus.CANCELLED;
import static org.folio.bulkops.domain.dto.DataImportStatus.COMMITTED;
import static org.folio.bulkops.domain.dto.DataImportStatus.ERROR;

import lombok.RequiredArgsConstructor;
import org.folio.bulkops.client.DataImportClient;
import org.folio.bulkops.client.MetadataProviderClient;
import org.folio.bulkops.domain.bean.JobLogEntry;
import org.folio.bulkops.domain.bean.RelatedInstanceInfo;
import org.folio.bulkops.domain.dto.DataImportJobExecution;
import org.folio.bulkops.domain.dto.DataImportProgress;
import org.folio.bulkops.domain.dto.DataImportStatus;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MetadataProviderService {
  private static final int CHUNK_SIZE = 1000;
  private final Set<DataImportStatus> completedStatuses = Set.of(COMMITTED, CANCELLED, ERROR);
  private final MetadataProviderClient metadataProviderClient;
  private final DataImportClient dataImportClient;

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

  public List<String> getUpdatedInstanceIds(List<DataImportJobExecution> jobExecutions) {
    return jobExecutions.stream()
      .map(DataImportJobExecution::getId)
      .map(UUID::toString)
      .map(this::getJobLogEntries)
      .flatMap(List::stream)
      .filter(entry -> UPDATED.equals(entry.getSourceRecordActionStatus()))
      .map(JobLogEntry::getRelatedInstanceInfo)
      .map(RelatedInstanceInfo::getIdList)
      .flatMap(List::stream)
      .toList();
  }

  private List<JobLogEntry> getJobLogEntries(String jobExecutionId) {
    var response = metadataProviderClient.getJobLogEntries(jobExecutionId, CHUNK_SIZE);
    if (response.getTotalRecords() <= CHUNK_SIZE) {
      return response.getEntries();
    }
    var result = new ArrayList<>(response.getEntries());
    var offset = CHUNK_SIZE;
    while (offset < response.getTotalRecords()) {
      result.addAll(metadataProviderClient.getJobLogEntries(jobExecutionId, offset, CHUNK_SIZE).getEntries());
      offset += CHUNK_SIZE;
    }
    return result;
  }
}
