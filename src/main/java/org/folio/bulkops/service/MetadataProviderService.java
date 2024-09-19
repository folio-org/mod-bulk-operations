package org.folio.bulkops.service;

import lombok.RequiredArgsConstructor;
import org.folio.bulkops.client.MetadataProviderClient;
import org.folio.bulkops.domain.dto.DataImportJobExecution;
import org.folio.bulkops.exception.NotFoundException;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MetadataProviderService {
  private static final String COMPOSITE_PARENT = "COMPOSITE_PARENT";
  private final MetadataProviderClient metadataProviderClient;

  public DataImportJobExecution getDataImportJobExecutionByJobProfileId(UUID dataImportJobProfileId) {
    var executions = metadataProviderClient.getJobExecutionsByJobProfileIdAndSubordinationType(dataImportJobProfileId, COMPOSITE_PARENT);
    if (executions.getJobExecutions().isEmpty()) {
      throw new NotFoundException("Job execution not found by jobProfileId " + dataImportJobProfileId);
    }
    return executions.getJobExecutions().get(0);
  }
}
