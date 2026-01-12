package org.folio.bulkops.service;

import static java.util.UUID.randomUUID;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.folio.bulkops.domain.dto.ApproachType.QUERY;
import static org.folio.bulkops.domain.dto.OperationStatusType.COMPLETED_WITH_ERRORS;
import static org.folio.bulkops.domain.dto.OperationStatusType.DATA_MODIFICATION;
import static org.folio.bulkops.domain.dto.OperationStatusType.FAILED;
import static org.folio.bulkops.domain.dto.OperationStatusType.RETRIEVING_RECORDS;
import static org.folio.bulkops.domain.dto.OperationStatusType.SAVED_IDENTIFIERS;
import static org.folio.bulkops.service.QueryService.QUERY_FILENAME_TEMPLATE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import org.folio.bulkops.client.QueryClient;
import org.folio.bulkops.client.RemoteFileSystemClient;
import org.folio.bulkops.domain.dto.ApproachType;
import org.folio.bulkops.domain.dto.EntityType;
import org.folio.bulkops.domain.entity.BulkOperation;
import org.folio.bulkops.domain.entity.BulkOperationExecutionContent;
import org.folio.bulkops.processor.permissions.check.PermissionsValidator;
import org.folio.bulkops.repository.BulkOperationRepository;
import org.folio.bulkops.util.FqmContentFetcher;
import org.folio.querytool.domain.dto.QueryDetails;
import org.folio.querytool.domain.dto.QueryDetails.StatusEnum;
import org.folio.spring.scope.FolioExecutionScopeExecutionContextManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class QueryServiceTest {

  @Mock private BulkOperationRepository bulkOperationRepository;
  @Mock private ErrorService errorService;
  @Mock private PermissionsValidator permissionsValidator;
  @Mock private RemoteFileSystemClient remoteFileSystemClient;
  @Mock private QueryClient queryClient;
  @Mock private FqmContentFetcher fqmContentFetcher;
  @Mock private LocalReferenceDataService localReferenceDataService;
  @Mock private SrsService srsService;

  private QueryService service;

  @BeforeEach
  void setUp() {
    ObjectMapper objectMapper = new ObjectMapper();
    service =
        new QueryService(
            bulkOperationRepository,
            errorService,
            objectMapper,
            permissionsValidator,
            remoteFileSystemClient,
            queryClient,
            fqmContentFetcher,
            localReferenceDataService,
            srsService);
  }

  @Test
  void shouldCallQueryClient() {
    var bulkOperation = BulkOperation.builder().id(randomUUID()).fqlQueryId(randomUUID()).build();

    var expected = new QueryDetails().status(QueryDetails.StatusEnum.SUCCESS).totalRecords(10);
    when(queryClient.getQuery(bulkOperation.getFqlQueryId(), true)).thenReturn(expected);

    var actual = service.getQueryResult(bulkOperation);

    assertThat(actual).isSameAs(expected);
    verify(queryClient).getQuery(bulkOperation.getFqlQueryId(), true);
  }

  @Test
  void shouldPersistCsvAndUpdateOperation() {
    var operationId = randomUUID();
    var fqlQueryId = randomUUID();
    var bulkOperation = BulkOperation.builder().id(operationId).fqlQueryId(fqlQueryId).build();

    when(queryClient.getSortedIds(fqlQueryId, 0, Integer.MAX_VALUE))
        .thenReturn(List.of(List.of("id1"), List.of("id2"), List.of("id1")));

    var expectedPath = String.format(QUERY_FILENAME_TEMPLATE, operationId);
    when(remoteFileSystemClient.put(any(ByteArrayInputStream.class), eq(expectedPath)))
        .thenReturn("link");

    service.saveIdentifiers(bulkOperation);

    verify(remoteFileSystemClient).put(any(ByteArrayInputStream.class), eq(expectedPath));
    verify(bulkOperationRepository).save(bulkOperation);

    assertThat(bulkOperation.getLinkToTriggeringCsvFile()).isEqualTo(expectedPath);
    assertThat(bulkOperation.getStatus()).isEqualTo(SAVED_IDENTIFIERS);
    assertThat(bulkOperation.getApproach()).isEqualTo(QUERY);
  }

  @Test
  void shouldFailAndSaveOperation_whenClientReturnsException() {
    var bulkOperation = BulkOperation.builder().id(randomUUID()).fqlQueryId(randomUUID()).build();

    when(queryClient.getSortedIds(any(), eq(0), eq(Integer.MAX_VALUE)))
        .thenThrow(new RuntimeException("boom"));

    service.saveIdentifiers(bulkOperation);

    verify(bulkOperationRepository).save(bulkOperation);

    assertThat(bulkOperation.getStatus()).isEqualTo(FAILED);
    assertThat(bulkOperation.getErrorMessage())
        .contains("Failed to save identifiers, reason: boom");
    assertThat(bulkOperation.getEndTime()).isNotNull();
  }

  @Test
  void shouldCompleteOperationDataModification_whenMatchedGreaterThanZero() throws Exception {
    var bulkOperation =
        BulkOperation.builder()
            .id(randomUUID())
            .entityType(EntityType.INSTANCE)
            .approach(QUERY)
            .build();

    var contents = new ArrayList<BulkOperationExecutionContent>();
    InputStream is = new ByteArrayInputStream("[]".getBytes());

    QueryService queryServiceSpy = spy(service);
    doAnswer(
            inv -> {
              bulkOperation.setMatchedNumOfRecords(2);
              bulkOperation.setProcessedNumOfRecords(2);
              return null;
            })
        .when(queryServiceSpy)
        .processAsyncQueryResult(
            any(),
            anyString(),
            anyString(),
            anyString(),
            anyString(),
            eq(bulkOperation),
            anySet(),
            eq(contents));

    queryServiceSpy.completeBulkOperation(is, bulkOperation, Set.of(), contents);

    assertThat(bulkOperation.getStatus()).isEqualTo(DATA_MODIFICATION);
    assertThat(bulkOperation.getEndTime()).isNotNull();

    assertThat(bulkOperation.getLinkToTriggeringCsvFile()).isNotBlank();
    assertThat(bulkOperation.getLinkToMatchedRecordsCsvFile()).isNotBlank();
    assertThat(bulkOperation.getLinkToMatchedRecordsJsonFile()).isNotBlank();

    verify(bulkOperationRepository).save(bulkOperation);
  }

  @Test
  void shouldCompleteOperationWithErrors_whenNoMatches() throws Exception {
    var bulkOperation =
        BulkOperation.builder()
            .id(randomUUID())
            .entityType(EntityType.INSTANCE)
            .approach(ApproachType.QUERY)
            .build();

    var contents = new ArrayList<BulkOperationExecutionContent>();
    InputStream is = new ByteArrayInputStream("[]".getBytes());

    QueryService queryServiceSpy = spy(service);
    doAnswer(
            inv -> {
              bulkOperation.setMatchedNumOfRecords(0);
              bulkOperation.setProcessedNumOfRecords(1);
              return null;
            })
        .when(queryServiceSpy)
        .processAsyncQueryResult(
            any(),
            anyString(),
            anyString(),
            anyString(),
            anyString(),
            eq(bulkOperation),
            anySet(),
            eq(contents));

    queryServiceSpy.completeBulkOperation(is, bulkOperation, Set.of(), contents);

    assertThat(bulkOperation.getStatus()).isEqualTo(COMPLETED_WITH_ERRORS);
    assertThat(bulkOperation.getEndTime()).isNotNull();

    verify(bulkOperationRepository).save(bulkOperation);
  }

  @Test
  void shouldFailOperationAndUploadErrors_whenProcessAsyncQueryResultThrows() throws Exception {
    var bulkOperation =
        BulkOperation.builder()
            .id(randomUUID())
            .entityType(EntityType.INSTANCE)
            .approach(ApproachType.QUERY)
            .build();

    var contents = new ArrayList<BulkOperationExecutionContent>();
    InputStream is = new ByteArrayInputStream("[]".getBytes());

    QueryService queryServiceSpy = spy(service);
    doThrow(new RuntimeException("kaboom"))
        .when(queryServiceSpy)
        .processAsyncQueryResult(
            any(),
            anyString(),
            anyString(),
            anyString(),
            anyString(),
            eq(bulkOperation),
            anySet(),
            eq(contents));

    when(errorService.uploadErrorsToStorage(any(), anyString(), anyString()))
        .thenReturn("errors-link");

    queryServiceSpy.completeBulkOperation(is, bulkOperation, Set.of(), contents);

    assertThat(bulkOperation.getStatus()).isEqualTo(FAILED);
    assertThat(bulkOperation.getErrorMessage()).contains("kaboom");
    assertThat(bulkOperation.getEndTime()).isNotNull();
    assertThat(bulkOperation.getLinkToMatchedRecordsErrorsCsvFile()).isEqualTo("errors-link");

    verify(errorService)
        .uploadErrorsToStorage(eq(bulkOperation.getId()), anyString(), contains("kaboom"));
    verify(bulkOperationRepository).save(bulkOperation);
  }

  @Test
  void shouldInvokeCompleteBulkOperation_whenContentsProvided() {
    var bulkOperation = new BulkOperation();
    bulkOperation.setId(randomUUID());
    bulkOperation.setEntityType(EntityType.ITEM);

    var uuids = List.of(randomUUID(), randomUUID(), randomUUID());

    InputStream is = new ByteArrayInputStream("[]".getBytes());

    QueryService queryServiceSpy = spy(service);
    when(fqmContentFetcher.contents(
            eq(uuids), eq(bulkOperation.getEntityType()), anyList(), eq(bulkOperation.getId())))
        .thenReturn(is);

    /* Mocking to simulate one UUID not found - No match found*/
    doNothing()
        .when(queryServiceSpy)
        .completeBulkOperation(
            any(InputStream.class), any(BulkOperation.class), anySet(), anyList());

    runWithFolioContext(
        () -> queryServiceSpy.retrieveRecordsIdentifiersFlowAsync(uuids, bulkOperation, List.of()));

    await().atMost(2, SECONDS).until(() -> bulkOperation.getStatus() == RETRIEVING_RECORDS);

    verify(bulkOperationRepository, atLeastOnce()).save(bulkOperation);

    verify(queryServiceSpy, times(1))
        .completeBulkOperation(same(is), same(bulkOperation), anySet(), anyList());
  }

  @Test
  void shouldFailAndNotInvokeCompleteBulkOperation_whenContentsThrows() {
    var bulkOperation = new BulkOperation();
    bulkOperation.setId(randomUUID());
    bulkOperation.setEntityType(EntityType.ITEM);
    var uuids = List.of(randomUUID());

    QueryService queryServiceSpy = spy(service);
    when(fqmContentFetcher.contents(anyList(), any(), anyList(), any()))
        .thenThrow(new RuntimeException("boom"));

    runWithFolioContext(
        () -> queryServiceSpy.retrieveRecordsIdentifiersFlowAsync(uuids, bulkOperation, List.of()));

    await().atMost(2, SECONDS).until(() -> bulkOperation.getStatus() == FAILED);

    assertNotNull(bulkOperation.getEndTime());
    assertTrue(bulkOperation.getErrorMessage().contains("FQM-based Identifiers Flow"));
    assertTrue(bulkOperation.getErrorMessage().contains("boom"));

    verify(queryServiceSpy, never()).completeBulkOperation(any(), any(), anySet(), anyList());
  }

  @Test
  void shouldFailAndNotCallFetch_withZeroRecords() {
    var bulkOperation = baseBulkOperation();

    QueryDetails queryDetails = mock(QueryDetails.class);
    QueryService queryServiceSpy = spy(service);
    when(queryDetails.getStatus()).thenReturn(StatusEnum.SUCCESS);
    when(queryDetails.getTotalRecords()).thenReturn(0);

    doReturn(queryDetails).when(queryServiceSpy).getQueryResult(bulkOperation);

    runWithFolioContext(() -> queryServiceSpy.retrieveRecordsQueryFlowAsync(bulkOperation));

    await().atMost(2, SECONDS).until(() -> bulkOperation.getStatus() == FAILED);

    assertEquals("No records found for the query", bulkOperation.getErrorMessage());

    verify(fqmContentFetcher, never()).fetch(any(), any(), anyInt(), anyList(), any());
    verify(queryServiceSpy, never()).completeBulkOperation(any(), any(), anySet(), anyList());
  }

  @Test
  void shouldCallFetchAndComplete() {
    var bulkOperation = baseBulkOperation();
    int total = 5;

    QueryDetails queryDetails = mock(QueryDetails.class);

    QueryService queryServiceSpy = spy(service);

    when(queryDetails.getStatus()).thenReturn(StatusEnum.SUCCESS);
    when(queryDetails.getTotalRecords()).thenReturn(total);

    doReturn(queryDetails).when(queryServiceSpy).getQueryResult(bulkOperation);
    doNothing()
        .when(queryServiceSpy)
        .completeBulkOperation(
            any(InputStream.class), any(BulkOperation.class), anySet(), anyList());

    InputStream is = new ByteArrayInputStream("[]".getBytes());
    when(fqmContentFetcher.fetch(
            eq(bulkOperation.getFqlQueryId()),
            eq(bulkOperation.getEntityType()),
            eq(total),
            anyList(),
            eq(bulkOperation.getId())))
        .thenReturn(is);

    var returned =
        runWithFolioContext(() -> queryServiceSpy.retrieveRecordsQueryFlowAsync(bulkOperation));

    assertSame(bulkOperation, returned);

    await().atMost(2, SECONDS).until(() -> bulkOperation.getStatus() == RETRIEVING_RECORDS);

    verify(bulkOperationRepository, atLeastOnce()).save(bulkOperation);

    verify(fqmContentFetcher, times(1))
        .fetch(
            eq(bulkOperation.getFqlQueryId()),
            eq(bulkOperation.getEntityType()),
            eq(total),
            anyList(),
            eq(bulkOperation.getId()));
    verify(queryServiceSpy, times(1))
        .completeBulkOperation(same(is), same(bulkOperation), anySet(), anyList());
    assertEquals(total, bulkOperation.getTotalNumOfRecords());
  }

  @Test
  void shouldFailAndNotCallComplete_whenFetchThrows() {
    var bulkOperation = baseBulkOperation();
    int total = 3;

    QueryDetails queryDetails = mock(QueryDetails.class);
    QueryService queryServiceSpy = spy(service);
    when(queryDetails.getStatus()).thenReturn(StatusEnum.SUCCESS);
    when(queryDetails.getTotalRecords()).thenReturn(total);

    doReturn(queryDetails).when(queryServiceSpy).getQueryResult(bulkOperation);

    when(fqmContentFetcher.fetch(
            eq(bulkOperation.getFqlQueryId()),
            eq(bulkOperation.getEntityType()),
            eq(total),
            anyList(),
            eq(bulkOperation.getId())))
        .thenThrow(new RuntimeException("boom"));

    runWithFolioContext(() -> queryServiceSpy.retrieveRecordsQueryFlowAsync(bulkOperation));

    await().atMost(2, SECONDS).until(() -> bulkOperation.getStatus() == FAILED);

    assertNotNull(bulkOperation.getEndTime());
    assertTrue(
        bulkOperation
            .getErrorMessage()
            .contains("Failed to save identifiers (FQM-based Query Flow)"));
    assertTrue(bulkOperation.getErrorMessage().contains("boom"));

    verify(queryServiceSpy, never()).completeBulkOperation(any(), any(), anySet(), anyList());
  }

  @ParameterizedTest
  @EnumSource(
      value = ApproachType.class,
      names = {"QUERY", "IN_APP"})
  void shouldCompleteBulkOperationWithMatchedRecords(ApproachType approachType) throws Exception {
    var bulkOperationId = UUID.fromString("915f86ba-4536-4f67-a6a4-59aa96a3d823");
    var bulkOperation =
        BulkOperation.builder()
            .id(bulkOperationId)
            .approach(approachType)
            .linkToTriggeringCsvFile(
                approachType != QUERY ? bulkOperationId + "/some-link.csv" : null)
            .build();
    var uuids = Set.of(randomUUID(), randomUUID());
    var contents = new ArrayList<BulkOperationExecutionContent>();
    InputStream is = new ByteArrayInputStream("[]".getBytes());

    var queryServiceSpy = spy(service);

    doAnswer(
            invocation -> {
              BulkOperation operation = invocation.getArgument(5);
              operation.setMatchedNumOfRecords(2);
              return null;
            })
        .when(queryServiceSpy)
        .processAsyncQueryResult(
            same(is),
            anyString(),
            anyString(),
            anyString(),
            anyString(),
            same(bulkOperation),
            same(uuids),
            same(contents));

    queryServiceSpy.completeBulkOperation(is, bulkOperation, uuids, contents);

    assertThat(bulkOperation.getStatus()).isEqualTo(DATA_MODIFICATION);

    if (approachType == QUERY) {
      assertThat(bulkOperation.getLinkToTriggeringCsvFile())
          .isEqualTo(
              "915f86ba-4536-4f67-a6a4-59aa96a3d823/"
                  + "Query-915f86ba-4536-4f67-a6a4-59aa96a3d823.csv");
      assertThat(bulkOperation.getLinkToMatchedRecordsCsvFile())
          .isEqualTo(
              "915f86ba-4536-4f67-a6a4-59aa96a3d823/"
                  + "2026-01-12-Matched-Records-Query-915f86ba-4536-4f67-a6a4-59aa96a3d823.csv");
    } else {
      assertThat(bulkOperation.getLinkToTriggeringCsvFile())
          .isEqualTo("915f86ba-4536-4f67-a6a4-59aa96a3d823/some-link.csv");
      assertThat(bulkOperation.getLinkToMatchedRecordsCsvFile())
          .isEqualTo(
              "915f86ba-4536-4f67-a6a4-59aa96a3d823/" + "2026-01-12-Matched-Records-some-link.csv");
      assertThat(bulkOperation.getLinkToMatchedRecordsJsonFile())
          .isEqualTo(
              "915f86ba-4536-4f67-a6a4-59aa96a3d823/"
                  + "json/2026-01-12-Matched-Records-some-link.json");
    }

    assertThat(bulkOperation.getEndTime()).isNotNull();
    verify(bulkOperationRepository).save(bulkOperation);
  }

  private BulkOperation baseBulkOperation() {
    var bulkOperation = new BulkOperation();
    bulkOperation.setId(randomUUID());
    bulkOperation.setFqlQueryId(randomUUID());
    bulkOperation.setEntityType(EntityType.ITEM);
    return bulkOperation;
  }

  private <T> T runWithFolioContext(Callable<T> callable) {
    try (MockedStatic<FolioExecutionScopeExecutionContextManager> ctx =
        mockStatic(FolioExecutionScopeExecutionContextManager.class)) {

      ctx.when(
              () ->
                  FolioExecutionScopeExecutionContextManager.getRunnableWithCurrentFolioContext(
                      any()))
          .thenAnswer(inv -> inv.getArgument(0));

      try {
        return callable.call();
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
  }

  private void runWithFolioContext(Runnable runnable) {
    try (MockedStatic<FolioExecutionScopeExecutionContextManager> ctx =
        mockStatic(FolioExecutionScopeExecutionContextManager.class)) {

      ctx.when(
              () ->
                  FolioExecutionScopeExecutionContextManager.getRunnableWithCurrentFolioContext(
                      any()))
          .thenAnswer(inv -> inv.getArgument(0));

      try {
        runnable.run();
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
  }
}
