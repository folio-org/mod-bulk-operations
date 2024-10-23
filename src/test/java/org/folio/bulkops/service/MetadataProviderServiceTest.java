package org.folio.bulkops.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import org.folio.bulkops.BaseTest;
import org.folio.bulkops.client.DataImportClient;
import org.folio.bulkops.client.MetadataProviderClient;
import org.folio.bulkops.domain.bean.JobLogEntry;
import org.folio.bulkops.domain.bean.JobLogEntryCollection;
import org.folio.bulkops.domain.bean.RelatedInstanceInfo;
import org.folio.bulkops.domain.bean.SplitStatus;
import org.folio.bulkops.domain.dto.DataImportJobExecution;
import org.folio.bulkops.domain.dto.DataImportJobExecutionCollection;
import org.folio.bulkops.domain.dto.DataImportProgress;
import org.folio.bulkops.domain.dto.DataImportStatus;
import org.folio.bulkops.domain.dto.ProfileInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

class MetadataProviderServiceTest extends BaseTest {
  @MockBean
  DataImportClient dataImportClient;
  @MockBean
  private MetadataProviderClient metadataProviderClient;

  @Autowired
  private MetadataProviderService metadataProviderService;

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void shouldGetJobExecutions(boolean splitStatusEnabled) {
    var jobProfileId = UUID.randomUUID();
    var compositeChildId = UUID.randomUUID();
    var parentSingleId = UUID.randomUUID();
    var executions = new DataImportJobExecutionCollection()
      .jobExecutions(List.of(new DataImportJobExecution()
        .id(compositeChildId)
        .subordinationType(DataImportJobExecution.SubordinationTypeEnum.COMPOSITE_PARENT),
        new DataImportJobExecution()
          .id(compositeChildId)
          .jobProfileInfo(new ProfileInfo().id(jobProfileId))
          .subordinationType(DataImportJobExecution.SubordinationTypeEnum.COMPOSITE_CHILD),
        new DataImportJobExecution()
          .id(parentSingleId)
          .jobProfileInfo(new ProfileInfo().id(jobProfileId))
          .subordinationType(DataImportJobExecution.SubordinationTypeEnum.PARENT_SINGLE)))
        .totalRecords(3);

    when(dataImportClient.getSplitStatus())
      .thenReturn(SplitStatus.builder().splitStatus(splitStatusEnabled).build());
    when(metadataProviderClient.getJobExecutionsByJobProfileId(jobProfileId, Integer.MAX_VALUE))
      .thenReturn(executions);

    var res = metadataProviderService.getJobExecutions(jobProfileId);

    if (splitStatusEnabled) {
      assertThat(res).hasSize(1);
      assertThat(res.get(0).getId()).isEqualTo(compositeChildId);
    } else {
      assertThat(res).hasSize(1);
      assertThat(res.get(0).getId()).isEqualTo(parentSingleId);
    }
  }

  @Test
  void shouldReturnDataImportJobCompletionWhenAllExecutionsCompleted() {
    var jobExecutions = List.of(
      new DataImportJobExecution().status(DataImportStatus.COMMITTED).totalJobParts(3),
      new DataImportJobExecution().status(DataImportStatus.CANCELLED),
      new DataImportJobExecution().status(DataImportStatus.ERROR)
    );

    assertTrue(metadataProviderService.isDataImportJobCompleted(jobExecutions));
  }

  @Test
  void shouldReturnDataImportJobIncompleteWhenSomeExecutionsNotCompleted() {
    var jobExecutions = List.of(
      new DataImportJobExecution().status(DataImportStatus.COMMITTED),
      new DataImportJobExecution().status(DataImportStatus.CANCELLED),
      new DataImportJobExecution().status(DataImportStatus.PARSING_IN_PROGRESS)
    );

    assertFalse(metadataProviderService.isDataImportJobCompleted(jobExecutions));
  }

  @Test
  void shouldReturnDataImportJobIncompleteIfListIsEmpty() {
    var jobExecutions = new ArrayList<DataImportJobExecution>();

    assertFalse(metadataProviderService.isDataImportJobCompleted(jobExecutions));
  }

  @Test
  void shouldCalculateProgress() {
    var jobExecutions = List.of(
      new DataImportJobExecution().progress(new DataImportProgress().current(10).total(100)),
      new DataImportJobExecution().progress(new DataImportProgress().current(10).total(100))
    );

    var progress = metadataProviderService.calculateProgress(jobExecutions);

    assertThat(progress.getCurrent()).isEqualTo(20);
    assertThat(progress.getTotal()).isEqualTo(200);
  }

  @Test
  void shouldGetUpdatedInstanceIds() {
    var id1 = UUID.randomUUID();
    var id2 = UUID.randomUUID();
    var updatedId1 = UUID.randomUUID().toString();
    var updatedId2 = UUID.randomUUID().toString();
    var jobExecutions = List.of(
      new DataImportJobExecution().id(id1),
      new DataImportJobExecution().id(id2)
    );

    when(metadataProviderClient.getJobLogEntries(id1.toString(), Integer.MAX_VALUE))
      .thenReturn(JobLogEntryCollection.builder()
        .entries(List.of(
          JobLogEntry.builder()
            .sourceRecordActionStatus("UPDATED")
            .relatedInstanceInfo(RelatedInstanceInfo.builder()
              .idList(List.of(updatedId1))
              .build())
            .build(),
          JobLogEntry.builder()
            .sourceRecordActionStatus("DISCARDED")
            .relatedInstanceInfo(RelatedInstanceInfo.builder()
              .idList(List.of(UUID.randomUUID().toString()))
              .build())
            .build()))
        .totalRecords(1)
        .build());
    when(metadataProviderClient.getJobLogEntries(id2.toString(), Integer.MAX_VALUE))
      .thenReturn(JobLogEntryCollection.builder()
        .entries(List.of(
          JobLogEntry.builder()
            .sourceRecordActionStatus("UPDATED")
            .relatedInstanceInfo(RelatedInstanceInfo.builder()
              .idList(List.of(updatedId2))
              .build())
            .build(),
          JobLogEntry.builder()
            .sourceRecordActionStatus("DISCARDED")
            .relatedInstanceInfo(RelatedInstanceInfo.builder()
              .idList(List.of(UUID.randomUUID().toString()))
              .build())
            .build()))
        .totalRecords(1)
        .build());

    var ids = metadataProviderService.getUpdatedInstanceIds(jobExecutions);

    assertThat(ids).hasSize(2);
    assertThat(ids.get(0)).isEqualTo(updatedId1);
    assertThat(ids.get(1)).isEqualTo(updatedId2);
  }
}
