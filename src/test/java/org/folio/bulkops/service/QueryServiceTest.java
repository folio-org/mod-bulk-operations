package org.folio.bulkops.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.folio.bulkops.domain.dto.OperationStatusType.CANCELLED;
import static org.folio.bulkops.domain.dto.OperationStatusType.COMPLETED_WITH_ERRORS;
import static org.folio.bulkops.domain.dto.OperationStatusType.EXECUTING_QUERY;
import static org.folio.bulkops.domain.dto.OperationStatusType.RETRIEVING_RECORDS;
import static org.folio.bulkops.service.QueryService.QUERY_FILENAME_TEMPLATE;
import static org.folio.bulkops.util.Constants.MULTIPLE_SRS;
import static org.folio.bulkops.util.Constants.SRS_MISSING;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import lombok.SneakyThrows;
import org.folio.bulkops.BaseTest;
import org.folio.bulkops.client.QueryClient;
import org.folio.bulkops.client.RemoteFileSystemClient;
import org.folio.bulkops.client.SrsClient;
import org.folio.bulkops.domain.bean.BulkOperationsEntity;
import org.folio.bulkops.domain.bean.User;
import org.folio.bulkops.domain.dto.EntityType;
import org.folio.bulkops.domain.dto.IdentifierType;
import org.folio.bulkops.domain.dto.OperationStatusType;
import org.folio.bulkops.domain.entity.BulkOperation;
import org.folio.bulkops.domain.entity.BulkOperationExecutionContent;
import org.folio.bulkops.exception.ReadPermissionException;
import org.folio.bulkops.processor.permissions.check.PermissionsValidator;
import org.folio.bulkops.processor.permissions.check.ReadPermissionsValidator;
import org.folio.bulkops.repository.BulkOperationRepository;
import org.folio.querytool.domain.dto.QueryDetails;
import org.folio.querytool.domain.dto.QueryIdentifier;
import org.folio.querytool.domain.dto.SubmitQuery;
import org.folio.spring.scope.FolioExecutionContextSetter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

class QueryServiceTest extends BaseTest {

  private final static String INSTANCE_JSON_PATH = "src/test/resources/files/instance.json";
  private final static String INSTANCE_MARC_JSON_PATH = "src/test/resources/files/instance_marc_1.json";

  @Autowired
  private QueryService queryService;

  @MockitoBean
  private QueryClient queryClient;
  @MockitoBean
  private RemoteFileSystemClient remoteFileSystemClient;
  @MockitoBean
  private BulkOperationRepository bulkOperationRepository;
  @MockitoSpyBean
  private ErrorService errorService;
  @MockitoBean
  private PermissionsValidator permissionsValidator;
  @MockitoBean
  private Writer writer;
  @MockitoBean
  private BulkOperationService bulkOperationService;
  @MockitoBean
  private ReadPermissionsValidator readPermissionsValidator;
  @MockitoBean
  private SrsClient srsClient;

  @Test
  void shouldSaveIdentifiersAndStartBulkOperationOnSuccessfulQueryExecution() {
    try (var context =  new FolioExecutionContextSetter(folioExecutionContext)) {
      var operationId = UUID.randomUUID();
      var fqlQueryId = UUID.randomUUID();
      var expectedPath = String.format(QUERY_FILENAME_TEMPLATE, operationId);
      var operation = BulkOperation.builder()
        .id(operationId)
        .fqlQueryId(fqlQueryId)
        .build();
      var queryDetails = new QueryDetails()
        .status(QueryDetails.StatusEnum.SUCCESS)
        .totalRecords(2)
        .content(List.of());

      when(queryClient.getQuery(fqlQueryId, true)).thenReturn(queryDetails);
      when(queryClient.getSortedIds(fqlQueryId, 0, Integer.MAX_VALUE))
        .thenReturn(Collections.singletonList(List.of(UUID.randomUUID().toString(), UUID.randomUUID().toString())));
      when(queryClient.executeQuery(any(SubmitQuery.class))).thenReturn(new QueryIdentifier().queryId(fqlQueryId));

      queryService.retrieveRecordsAndCheckQueryExecutionStatus(operation);

      await().untilAsserted(() ->
        verify(remoteFileSystemClient).put(any(ByteArrayInputStream.class), eq(expectedPath)));
    }
  }

  @ParameterizedTest
  @EnumSource(value = QueryDetails.StatusEnum.class, names = {"SUCCESS", "FAILED"}, mode = EnumSource.Mode.INCLUDE)
  void shouldFailOperationIfNoMatchFoundOrQueryFails(QueryDetails.StatusEnum status) {
    try (var context =  new FolioExecutionContextSetter(folioExecutionContext)) {
      var operationId = UUID.randomUUID();
      var fqlQueryId = UUID.randomUUID();
      var expectedPath = String.format(QUERY_FILENAME_TEMPLATE, operationId);
      var operation = BulkOperation.builder()
        .id(operationId)
        .fqlQueryId(fqlQueryId)
        .build();
      var queryDetails = new QueryDetails()
        .status(status)
        .failureReason("some reason")
        .totalRecords(0);

      when(queryClient.getQuery(fqlQueryId, true)).thenReturn(queryDetails);
      when(queryClient.executeQuery(any(SubmitQuery.class))).thenReturn(new QueryIdentifier().queryId(fqlQueryId));

      queryService.retrieveRecordsAndCheckQueryExecutionStatus(operation);

      verify(remoteFileSystemClient, times(0)).put(any(ByteArrayInputStream.class), eq(expectedPath));
      var operationCaptor = ArgumentCaptor.forClass(BulkOperation.class);
      await().untilAsserted(() -> verify(bulkOperationRepository, times(2)).save(operationCaptor.capture()));
      assertThat(operationCaptor.getValue().getStatus()).isEqualTo(OperationStatusType.FAILED);
      assertThat(operationCaptor.getValue().getEndTime()).isNotNull();
    }
  }

  @Test
  void shouldCancelOperationIfQueryWasCancelled() {
    try (var context =  new FolioExecutionContextSetter(folioExecutionContext)) {
      var operationId = UUID.randomUUID();
      var fqlQueryId = UUID.randomUUID();
      var operation = BulkOperation.builder()
        .id(operationId)
        .fqlQueryId(fqlQueryId)
        .build();
      var queryDetails = new QueryDetails()
        .status(QueryDetails.StatusEnum.CANCELLED);

      when(queryClient.getQuery(fqlQueryId, true)).thenReturn(queryDetails);
      when(queryClient.executeQuery(any(SubmitQuery.class))).thenReturn(new QueryIdentifier().queryId(fqlQueryId));

      queryService.retrieveRecordsAndCheckQueryExecutionStatus(operation);

      var operationCaptor = ArgumentCaptor.forClass(BulkOperation.class);
      await().untilAsserted(() -> verify(bulkOperationRepository, times(2)).save(operationCaptor.capture()));
      assertThat(operationCaptor.getValue().getStatus()).isEqualTo(CANCELLED);
    }
  }

  @Test
  void shouldReturnOperationIfQueryInProgress() {
    try (var context =  new FolioExecutionContextSetter(folioExecutionContext)) {
      var operationId = UUID.randomUUID();
      var fqlQueryId = UUID.randomUUID();
      var operation = BulkOperation.builder()
        .id(operationId)
        .fqlQueryId(fqlQueryId)
        .status(EXECUTING_QUERY)
        .build();
      var queryDetails = new QueryDetails()
        .status(QueryDetails.StatusEnum.IN_PROGRESS);

      when(queryClient.getQuery(fqlQueryId, true)).thenReturn(queryDetails);
      when(queryClient.executeQuery(any(SubmitQuery.class))).thenReturn(new QueryIdentifier().queryId(fqlQueryId));

      var result = queryService.retrieveRecordsAndCheckQueryExecutionStatus(operation);

      assertThat(result.getStatus()).isEqualTo(RETRIEVING_RECORDS);
    }
  }

  @Test
  @SneakyThrows
  void shouldStartQueryOperation() {
    try (var context =  new FolioExecutionContextSetter(folioExecutionContext)) {
      var operation = BulkOperation.builder().id(UUID.randomUUID())
        .status(OperationStatusType.EXECUTING_QUERY).approach(org.folio.bulkops.domain.dto.ApproachType.QUERY)
        .entityType(org.folio.bulkops.domain.dto.EntityType.INSTANCE)
        .identifierType(IdentifierType.ID)
        .build();
      var queryId = UUID.randomUUID();
      var instanceJsonb = Files.readString(Path.of(INSTANCE_JSON_PATH));
      var queryDetails = new QueryDetails().content(List.of(Map.of("instance.jsonb", instanceJsonb,
          "instance.id", "69640328-788e-43fc-9c3c-af39e243f3b7")))
        .status(QueryDetails.StatusEnum.SUCCESS).totalRecords(1);

      when(queryClient.executeQuery(any(SubmitQuery.class))).thenReturn(new QueryIdentifier().queryId(queryId));
      when(queryClient.getQuery(queryId, true)).thenReturn(queryDetails);
      when(bulkOperationRepository.save(any(BulkOperation.class))).thenReturn(operation);
      when(userClient.getUserById(any(String.class))).thenReturn(User.builder().username("username").build());
      when(readPermissionsValidator.isBulkEditReadPermissionExists("diku", EntityType.INSTANCE)).thenReturn(true);
      when(remoteFileSystemClient.writer(any(String.class))).thenReturn(writer);

      queryService.retrieveRecordsAndCheckQueryExecutionStatus(operation);

      var operationCaptor = ArgumentCaptor.forClass(BulkOperation.class);
      await().untilAsserted(() -> verify(bulkOperationRepository, times(5)).save(operationCaptor.capture()));
      assertThat(operationCaptor.getValue().getStatus()).isEqualTo(OperationStatusType.DATA_MODIFICATION);

      assertThat(operationCaptor.getValue().getEndTime()).isNotNull();
      assertThat(operationCaptor.getValue().getLinkToMatchedRecordsJsonFile()).isNotNull();
      assertThat(operationCaptor.getValue().getLinkToMatchedRecordsCsvFile()).isNotNull();
    }
  }

  @Test
  @SneakyThrows
  void shouldThrowReadExceptionWhenNoPermission() {
    try (var context =  new FolioExecutionContextSetter(folioExecutionContext)) {
      var operation = BulkOperation.builder().id(UUID.randomUUID())
        .status(OperationStatusType.EXECUTING_QUERY).approach(org.folio.bulkops.domain.dto.ApproachType.QUERY)
        .entityType(org.folio.bulkops.domain.dto.EntityType.INSTANCE)
        .build();
      var queryId = UUID.randomUUID();
      var instanceJsonb = Files.readString(Path.of(INSTANCE_JSON_PATH));
      var queryDetails = new QueryDetails().content(List.of(Map.of("instance.jsonb", instanceJsonb,
                      "instance.id", "69640328-788e-43fc-9c3c-af39e243f3b7")))
        .status(QueryDetails.StatusEnum.SUCCESS).totalRecords(1);

      when(queryClient.executeQuery(any(SubmitQuery.class))).thenReturn(new QueryIdentifier().queryId(queryId));
      when(queryClient.getQuery(queryId, true)).thenReturn(queryDetails);
      when(bulkOperationRepository.save(any(BulkOperation.class))).thenReturn(operation);
      when(userClient.getUserById(any(String.class))).thenReturn(User.builder().username("username").build());
      doThrow(new ReadPermissionException("User username does not have required permission to view the instance record - id=69640328-788e-43fc-9c3c-af39e243f3b7 on the tenant diku", "69640328-788e-43fc-9c3c-af39e243f3b7"))
              .when(permissionsValidator).checkPermissions(any(BulkOperation.class), any(BulkOperationsEntity.class));

      queryService.retrieveRecordsAndCheckQueryExecutionStatus(operation);

      await().untilAsserted(() -> verify(bulkOperationRepository, times(5))
              .save(ArgumentCaptor.forClass(BulkOperation.class).capture()));

      var operationCaptor = ArgumentCaptor.forClass(BulkOperation.class);
      var executionContentsCaptor = ArgumentCaptor.forClass(List.class);
      verify(errorService).saveErrorsAfterQuery(executionContentsCaptor.capture(), operationCaptor.capture());
      assertThat(((BulkOperationExecutionContent)executionContentsCaptor.getValue().getFirst()).getErrorMessage())
        .isEqualTo("User username does not have required permission to view the instance record - id=69640328-788e-43fc-9c3c-af39e243f3b7 on the tenant diku");
      assertThat(operationCaptor.getValue().getStatus()).isEqualTo(COMPLETED_WITH_ERRORS);
      assertThat(operationCaptor.getValue().getTotalNumOfRecords()).isEqualTo(1);
      assertThat(operationCaptor.getValue().getProcessedNumOfRecords()).isEqualTo(1);
      assertThat(operationCaptor.getValue().getMatchedNumOfErrors()).isEqualTo(1);
      assertThat(operationCaptor.getValue().getMatchedNumOfRecords()).isEqualTo(0);
    }
  }

  @Test
  @SneakyThrows
  void shouldThrowSrsMissingExceptionWhenNoSrs() {
    try (var context =  new FolioExecutionContextSetter(folioExecutionContext)) {
      var operation = BulkOperation.builder().id(UUID.randomUUID())
              .status(OperationStatusType.EXECUTING_QUERY).approach(org.folio.bulkops.domain.dto.ApproachType.QUERY)
              .entityType(EntityType.INSTANCE_MARC)
              .build();
      var queryId = UUID.randomUUID();
      var instanceJsonb = Files.readString(Path.of(INSTANCE_MARC_JSON_PATH));
      var queryDetails = new QueryDetails().content(List.of(Map.of("instance.jsonb", instanceJsonb,
                      "instance.id", "69640328-788e-43fc-9c3c-af39e243f3b7")))
              .status(QueryDetails.StatusEnum.SUCCESS).totalRecords(1);

      when(queryClient.executeQuery(any(SubmitQuery.class))).thenReturn(new QueryIdentifier().queryId(queryId));
      when(queryClient.getQuery(queryId, true)).thenReturn(queryDetails);
      when(bulkOperationRepository.save(any(BulkOperation.class))).thenReturn(operation);
      when(userClient.getUserById(any(String.class))).thenReturn(User.builder().username("username").build());
      var srsRecordsNode = objectMapper.createObjectNode();
      srsRecordsNode.set("sourceRecords", objectMapper.valueToTree(List.of()));
      when(srsClient.getMarc(anyString(), anyString(), anyBoolean())).thenReturn(srsRecordsNode);

      queryService.retrieveRecordsAndCheckQueryExecutionStatus(operation);

      await().untilAsserted(() -> verify(bulkOperationRepository, times(5))
              .save(ArgumentCaptor.forClass(BulkOperation.class).capture()));

      var operationCaptor = ArgumentCaptor.forClass(BulkOperation.class);
      var executionContentsCaptor = ArgumentCaptor.forClass(List.class);
      verify(errorService).saveErrorsAfterQuery(executionContentsCaptor.capture(), operationCaptor.capture());
      assertThat(((BulkOperationExecutionContent)executionContentsCaptor.getValue().getFirst()).getErrorMessage())
              .isEqualTo(SRS_MISSING);
      assertThat(operationCaptor.getValue().getStatus()).isEqualTo(COMPLETED_WITH_ERRORS);
      assertThat(operationCaptor.getValue().getTotalNumOfRecords()).isEqualTo(1);
      assertThat(operationCaptor.getValue().getProcessedNumOfRecords()).isEqualTo(1);
      assertThat(operationCaptor.getValue().getMatchedNumOfErrors()).isEqualTo(1);
      assertThat(operationCaptor.getValue().getMatchedNumOfRecords()).isEqualTo(0);
    }
  }

  @Test
  @SneakyThrows
  void shouldThrowMultipleSrsExceptionWhenMoreThanOneMarc() {
    try (var context =  new FolioExecutionContextSetter(folioExecutionContext)) {
      var operation = BulkOperation.builder().id(UUID.randomUUID())
              .status(OperationStatusType.EXECUTING_QUERY).approach(org.folio.bulkops.domain.dto.ApproachType.QUERY)
              .entityType(EntityType.INSTANCE_MARC)
              .build();
      var queryId = UUID.randomUUID();
      var instanceJsonb = Files.readString(Path.of(INSTANCE_MARC_JSON_PATH));
      var queryDetails = new QueryDetails().content(List.of(Map.of("instance.jsonb", instanceJsonb,
                      "instance.id", "69640328-788e-43fc-9c3c-af39e243f3b7")))
              .status(QueryDetails.StatusEnum.SUCCESS).totalRecords(1);
      String srsJson = """
              {
                "sourceRecords": [
                    { "recordId": "22240328-788e-43fc-9c3c-af39e243f3b7" },
                    { "recordId": "33340328-788e-43fc-9c3c-af39e243f3b7" }
                  ]
              }
              """;

      when(queryClient.executeQuery(any(SubmitQuery.class))).thenReturn(new QueryIdentifier().queryId(queryId));
      when(queryClient.getQuery(queryId, true)).thenReturn(queryDetails);
      when(bulkOperationRepository.save(any(BulkOperation.class))).thenReturn(operation);
      when(userClient.getUserById(any(String.class))).thenReturn(User.builder().username("username").build());
      var srsRecordsNode = objectMapper.readTree(srsJson);
      when(srsClient.getMarc(anyString(), anyString(), anyBoolean())).thenReturn(srsRecordsNode);

      queryService.retrieveRecordsAndCheckQueryExecutionStatus(operation);

      await().untilAsserted(() -> verify(bulkOperationRepository, times(5))
              .save(ArgumentCaptor.forClass(BulkOperation.class).capture()));

      var operationCaptor = ArgumentCaptor.forClass(BulkOperation.class);
      var executionContentsCaptor = ArgumentCaptor.forClass(List.class);
      verify(errorService).saveErrorsAfterQuery(executionContentsCaptor.capture(), operationCaptor.capture());
      assertThat(((BulkOperationExecutionContent)executionContentsCaptor.getValue().getFirst()).getErrorMessage())
              .isEqualTo(MULTIPLE_SRS.formatted("22240328-788e-43fc-9c3c-af39e243f3b7, 33340328-788e-43fc-9c3c-af39e243f3b7"));
      assertThat(operationCaptor.getValue().getStatus()).isEqualTo(COMPLETED_WITH_ERRORS);
      assertThat(operationCaptor.getValue().getTotalNumOfRecords()).isEqualTo(1);
      assertThat(operationCaptor.getValue().getProcessedNumOfRecords()).isEqualTo(1);
      assertThat(operationCaptor.getValue().getMatchedNumOfErrors()).isEqualTo(1);
      assertThat(operationCaptor.getValue().getMatchedNumOfRecords()).isEqualTo(0);
    }
  }
}
