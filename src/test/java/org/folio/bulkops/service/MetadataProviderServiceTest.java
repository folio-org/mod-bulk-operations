package org.folio.bulkops.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import org.folio.bulkops.BaseTest;
import org.folio.bulkops.client.MetadataProviderClient;
import org.folio.bulkops.domain.dto.DataImportJobExecution;
import org.folio.bulkops.domain.dto.DataImportJobExecutionCollection;
import org.folio.bulkops.exception.NotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.Collections;
import java.util.UUID;

class MetadataProviderServiceTest extends BaseTest {
  @MockBean
  private MetadataProviderClient metadataProviderClient;

  @Autowired
  private MetadataProviderService metadataProviderService;

  @Test
  void shouldGetDataImportJobExecutionByJobProfileId() {
    var jobExecution = new DataImportJobExecution().id(UUID.randomUUID());
    when(metadataProviderClient.getJobExecutionsByJobProfileIdAndSubordinationType(any(UUID.class), anyString()))
      .thenReturn(new DataImportJobExecutionCollection()
        .jobExecutions(Collections.singletonList(jobExecution))
        .totalRecords(1));

    var result = metadataProviderService.getDataImportJobExecutionByJobProfileId(UUID.randomUUID());

    assertThat(result).isNotNull();
  }

  @Test
  void shouldThrowNotFoundExceptionIfJobExecutionWasNotFound() {
    when(metadataProviderClient.getJobExecutionsByJobProfileIdAndSubordinationType(any(UUID.class), anyString()))
      .thenThrow(new NotFoundException("not found"));

    var id = UUID.randomUUID();
    var throwable = assertThrows(NotFoundException.class, () -> metadataProviderService.getDataImportJobExecutionByJobProfileId(id));
    assertThat(throwable.getMessage()).isEqualTo("not found");
  }
}
