package org.folio.bulkops.service;

import static java.util.Objects.nonNull;

import lombok.RequiredArgsConstructor;
import org.folio.bulkops.client.MetadataProviderClient;
import org.folio.bulkops.domain.dto.DataImportJobExecution;
import org.folio.bulkops.domain.dto.DataImportProgress;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MetadataProviderService {
  private static final String COMPOSITE_PARENT = "COMPOSITE_PARENT";
  private final MetadataProviderClient metadataProviderClient;

  public DataImportJobExecution getDataImportJobExecutionByJobProfileId(UUID dataImportJobProfileId) {
    var executions = metadataProviderClient.getJobExecutionsByJobProfileIdAndSubordinationType(dataImportJobProfileId, COMPOSITE_PARENT)
        .getJobExecutions().stream()
        .filter(jobExecution -> nonNull(jobExecution.getJobProfileInfo()))
        .filter(jobExecution -> jobExecution.getJobProfileInfo().getId().equals(dataImportJobProfileId))
        .toList();

    return executions.isEmpty() ?
      new DataImportJobExecution().progress(new DataImportProgress().current(0).total(0)) :
      executions.get(0);
  }
}
