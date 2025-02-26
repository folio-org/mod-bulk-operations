package org.folio.bulkops.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.folio.bulkops.domain.dto.OperationStatusType.CANCELLED;
import static org.folio.bulkops.domain.dto.OperationStatusType.EXECUTING_QUERY;
import static org.folio.bulkops.service.QueryService.QUERY_FILENAME_TEMPLATE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.folio.bulkops.BaseTest;
import org.folio.bulkops.client.QueryClient;
import org.folio.bulkops.client.RemoteFileSystemClient;
import org.folio.bulkops.domain.dto.OperationStatusType;
import org.folio.bulkops.domain.entity.BulkOperation;
import org.folio.bulkops.repository.BulkOperationRepository;
import org.folio.querytool.domain.dto.QueryDetails;
import org.folio.spring.scope.FolioExecutionContextSetter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.io.ByteArrayInputStream;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

class QueryServiceTest extends BaseTest {
   @MockitoBean
  private QueryClient queryClient;
   @MockitoBean
  private BulkOperationRepository bulkOperationRepository;
   @MockitoBean
  private RemoteFileSystemClient remoteFileSystemClient;
  @Autowired
  private QueryService queryService;

  @Test
  void shouldSaveIdentifiersOnSuccessfulQueryExecution() {
    try (var context =  new FolioExecutionContextSetter(folioExecutionContext)) {
      var operationId = UUID.randomUUID();
      var fqlQueryId = UUID.randomUUID();
      var expectedPath = String.format(QUERY_FILENAME_TEMPLATE, operationId);
      var operation = BulkOperation.builder()
        .id(operationId)
        .fqlQueryId(fqlQueryId)
        .build();

      when(queryClient.getQuery(fqlQueryId)).thenReturn(new QueryDetails()
        .status(QueryDetails.StatusEnum.SUCCESS)
        .totalRecords(2));
      when(queryClient.getSortedIds(fqlQueryId, 0, Integer.MAX_VALUE))
        .thenReturn(Collections.singletonList(List.of(UUID.randomUUID().toString(), UUID.randomUUID().toString())));

      queryService.checkQueryExecutionStatus(operation);

      await().untilAsserted(() ->
        verify(remoteFileSystemClient).put(any(ByteArrayInputStream.class), eq(expectedPath)));
    }
  }

  @ParameterizedTest
  @EnumSource(value = QueryDetails.StatusEnum.class, names = {"SUCCESS", "FAILED"}, mode = EnumSource.Mode.INCLUDE)
  void shouldFailOperationIfNoMatchFoundOrQueryFails(QueryDetails.StatusEnum status) {
    var operationId = UUID.randomUUID();
    var fqlQueryId = UUID.randomUUID();
    var expectedPath = String.format(QUERY_FILENAME_TEMPLATE, operationId);
    var operation = BulkOperation.builder()
      .id(operationId)
      .fqlQueryId(fqlQueryId)
      .build();

    when(queryClient.getQuery(fqlQueryId)).thenReturn(new QueryDetails()
      .status(status)
      .failureReason("some reason")
      .totalRecords(0));

    queryService.checkQueryExecutionStatus(operation);

    verify(remoteFileSystemClient, times(0)).put(any(ByteArrayInputStream.class), eq(expectedPath));
    var operationCaptor = ArgumentCaptor.forClass(BulkOperation.class);
    verify(bulkOperationRepository).save(operationCaptor.capture());
    assertThat(operationCaptor.getValue().getStatus()).isEqualTo(OperationStatusType.FAILED);
    assertThat(operationCaptor.getValue().getEndTime()).isNotNull();
  }

  @Test
  void shouldCancelOperationIfQueryWasCancelled() {
    var operationId = UUID.randomUUID();
    var fqlQueryId = UUID.randomUUID();
    var operation = BulkOperation.builder()
      .id(operationId)
      .fqlQueryId(fqlQueryId)
      .build();

    when(queryClient.getQuery(fqlQueryId)).thenReturn(new QueryDetails()
      .status(QueryDetails.StatusEnum.CANCELLED));

    queryService.checkQueryExecutionStatus(operation);

    var operationCaptor = ArgumentCaptor.forClass(BulkOperation.class);
    verify(bulkOperationRepository).save(operationCaptor.capture());
    assertThat(operationCaptor.getValue().getStatus()).isEqualTo(CANCELLED);
  }

  @Test
  void shouldReturnOperationIfQueryInProgress() {
    var operationId = UUID.randomUUID();
    var fqlQueryId = UUID.randomUUID();
    var operation = BulkOperation.builder()
      .id(operationId)
      .fqlQueryId(fqlQueryId)
      .status(EXECUTING_QUERY)
      .build();

    when(queryClient.getQuery(fqlQueryId)).thenReturn(new QueryDetails()
      .status(QueryDetails.StatusEnum.IN_PROGRESS));

    var result = queryService.checkQueryExecutionStatus(operation);

    assertThat(result.getStatus()).isEqualTo(EXECUTING_QUERY);
  }
}
