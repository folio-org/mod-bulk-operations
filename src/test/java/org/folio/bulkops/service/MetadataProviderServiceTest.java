package org.folio.bulkops.service;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.bulkops.domain.bean.JobLogEntry.ActionStatus.DISCARDED;
import static org.folio.bulkops.domain.bean.JobLogEntry.ActionStatus.UPDATED;
import static org.folio.bulkops.service.MetadataProviderService.MSG_FAILED_TO_GET_LOG_ENTRIES_CHUNK;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
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
import org.folio.bulkops.domain.dto.ErrorType;
import org.folio.bulkops.domain.dto.ProfileInfo;
import org.folio.bulkops.domain.entity.BulkOperation;
import org.folio.bulkops.exception.NotFoundException;
import org.folio.spring.scope.FolioExecutionContextSetter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

class MetadataProviderServiceTest extends BaseTest {
   @MockitoBean
  DataImportClient dataImportClient;
   @MockitoBean
  private MetadataProviderClient metadataProviderClient;
   @MockitoBean
  private ErrorService errorService;

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
  void shouldFetchUpdatedInstanceIds() {
    var updatedId1 = UUID.randomUUID().toString();
    var updatedId2 = UUID.randomUUID().toString();
    var logEntries = List.of(
      JobLogEntry.builder()
        .sourceRecordActionStatus(UPDATED)
        .relatedInstanceInfo(RelatedInstanceInfo.builder()
          .idList(List.of(updatedId1))
          .build())
        .build(),
      JobLogEntry.builder()
        .sourceRecordActionStatus(DISCARDED)
        .relatedInstanceInfo(RelatedInstanceInfo.builder()
          .idList(List.of(UUID.randomUUID().toString()))
          .build())
        .build(),
      JobLogEntry.builder()
        .sourceRecordActionStatus(UPDATED)
        .relatedInstanceInfo(RelatedInstanceInfo.builder()
          .idList(List.of(updatedId2))
          .build())
        .build(),
      JobLogEntry.builder()
        .sourceRecordActionStatus(DISCARDED)
        .relatedInstanceInfo(RelatedInstanceInfo.builder()
          .idList(List.of(UUID.randomUUID().toString()))
          .build())
        .build());

    var ids = metadataProviderService.fetchUpdatedInstanceIds(logEntries);

    assertThat(ids).hasSize(2);
    assertThat(ids.get(0)).isEqualTo(updatedId1);
    assertThat(ids.get(1)).isEqualTo(updatedId2);
  }

  @Test
  void shouldRetrieveDataImportJobLogEntriesInChunks() {
    try (var context =  new FolioExecutionContextSetter(folioExecutionContext)) {
      var dataImportJobId = UUID.randomUUID();

      when(metadataProviderClient.getJobLogEntries(dataImportJobId.toString(), 1))
        .thenReturn(JobLogEntryCollection.builder()
          .entries(Collections.emptyList())
          .totalRecords(10)
          .build());
      var threeEntriesCollection = JobLogEntryCollection.builder()
        .entries(List.of(
          JobLogEntry.builder().build(),
          JobLogEntry.builder()
            .error("error")
            .relatedInstanceInfo(RelatedInstanceInfo.builder()
              .hridList(List.of("123"))
              .build())
            .build(),
          JobLogEntry.builder().build()
        ))
        .totalRecords(10)
        .build();
      var singleEntryCollection = JobLogEntryCollection.builder()
        .entries(Collections.singletonList(JobLogEntry.builder().build()))
        .totalRecords(10)
        .build();
      when(metadataProviderClient.getJobLogEntries(eq(dataImportJobId.toString()), anyLong(), anyLong()))
        .thenAnswer(invocation -> {
          var offset = (Long) invocation.getArguments()[1];
          return switch (offset.intValue()) {
            case 0, 3, 6 -> threeEntriesCollection;
            case 9 -> singleEntryCollection;
            default -> throw new IllegalStateException("Unexpected value: " + offset);
          };
        });
      var executions = Collections.singletonList(new DataImportJobExecution().id(dataImportJobId));
      var operation = new BulkOperation();

      var entries = metadataProviderService.getJobLogEntries(operation, executions);

      assertThat(entries).hasSize(10);

      verify(metadataProviderClient).getJobLogEntries(dataImportJobId.toString(), 0, 3);
      verify(metadataProviderClient).getJobLogEntries(dataImportJobId.toString(), 3, 3);
      verify(metadataProviderClient).getJobLogEntries(dataImportJobId.toString(), 6, 3);
      verify(metadataProviderClient).getJobLogEntries(dataImportJobId.toString(), 9, 3);
    }
  }

  @Test
  void shouldSaveErrorAndCompleteRetrievingJobLogEntriesWhenChunkFails() {
    try (var context =  new FolioExecutionContextSetter(folioExecutionContext)) {
      var dataImportJobId = UUID.randomUUID();
      var operationId = UUID.randomUUID();

      when(metadataProviderClient.getJobLogEntries(dataImportJobId.toString(), 1))
        .thenReturn(JobLogEntryCollection.builder()
          .entries(Collections.emptyList())
          .totalRecords(10)
          .build());

      var threeEntriesCollection = JobLogEntryCollection.builder()
        .entries(List.of(
          JobLogEntry.builder().build(),
          JobLogEntry.builder()
            .error("error")
            .relatedInstanceInfo(RelatedInstanceInfo.builder()
              .hridList(List.of("123"))
              .build())
            .build(),
          JobLogEntry.builder().build()
        ))
        .totalRecords(10)
        .build();
      var singleEntryCollection = JobLogEntryCollection.builder()
        .entries(Collections.singletonList(JobLogEntry.builder().build()))
        .totalRecords(10)
        .build();
      when(metadataProviderClient.getJobLogEntries(eq(dataImportJobId.toString()), anyLong(), anyLong()))
        .thenAnswer(invocation -> {
          var offset = (Long) invocation.getArguments()[1];
          return switch (offset.intValue()) {
            case 0, 6 -> threeEntriesCollection;
            case 3 -> throw new NotFoundException("Log entries were not found");
            case 9 -> singleEntryCollection;
            default -> throw new IllegalStateException("Unexpected offset value: " + offset);
          };
        });

      var executions = Collections.singletonList(new DataImportJobExecution().id(dataImportJobId));
      var operation = BulkOperation.builder().id(operationId).build();

      var entries = metadataProviderService.getJobLogEntries(operation, executions);

      assertThat(entries).hasSize(7);

      verify(metadataProviderClient).getJobLogEntries(dataImportJobId.toString(), 0, 3);
      verify(metadataProviderClient).getJobLogEntries(dataImportJobId.toString(), 3, 3);
      verify(metadataProviderClient).getJobLogEntries(dataImportJobId.toString(), 6, 3);
      verify(metadataProviderClient).getJobLogEntries(dataImportJobId.toString(), 9, 3);

      var expectedMessage = MSG_FAILED_TO_GET_LOG_ENTRIES_CHUNK.formatted(3, "Log entries were not found");
      verify(errorService).saveError(operationId, EMPTY, expectedMessage, ErrorType.ERROR);
    }
  }
}
