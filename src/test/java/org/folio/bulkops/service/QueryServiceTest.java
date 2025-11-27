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
import static org.folio.bulkops.util.MarcValidator.INVALID_MARC_MESSAGE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.SneakyThrows;
import org.folio.bulkops.BaseTest;
import org.folio.bulkops.client.QueryClient;
import org.folio.bulkops.client.RemoteFileSystemClient;
import org.folio.bulkops.client.SrsClient;
import org.folio.bulkops.domain.bean.BulkOperationsEntity;
import org.folio.bulkops.domain.bean.InstanceFormat;
import org.folio.bulkops.domain.bean.InstanceStatus;
import org.folio.bulkops.domain.bean.InstanceType;
import org.folio.bulkops.domain.bean.ModeOfIssuance;
import org.folio.bulkops.domain.bean.NatureOfContentTerm;
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
import org.folio.bulkops.util.FqmContentFetcher;
import org.folio.querytool.domain.dto.QueryDetails;
import org.folio.querytool.domain.dto.QueryIdentifier;
import org.folio.querytool.domain.dto.SubmitQuery;
import org.folio.spring.scope.FolioExecutionContextSetter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

class QueryServiceTest extends BaseTest {

  private static final String INSTANCE_JSON_PATH = "src/test/resources/files/instance.json";
  private static final String INSTANCE_MARC_JSON_PATH =
      "src/test/resources/files/instance_marc_1.json";

  @Autowired private QueryService queryService;

  @MockitoBean private QueryClient queryClient;
  @MockitoBean private RemoteFileSystemClient remoteFileSystemClient;
  @MockitoBean private BulkOperationRepository bulkOperationRepository;
  @MockitoSpyBean private ErrorService errorService;
  @MockitoBean private PermissionsValidator permissionsValidator;
  @MockitoBean private Writer writer;
  @MockitoBean private BulkOperationService bulkOperationService;
  @MockitoBean private ReadPermissionsValidator readPermissionsValidator;
  @MockitoBean private SrsClient srsClient;
  @MockitoBean private FqmContentFetcher fqmContentFetcher;

  @ParameterizedTest
  @EnumSource(
      value = QueryDetails.StatusEnum.class,
      names = {"SUCCESS", "FAILED"},
      mode = EnumSource.Mode.INCLUDE)
  void shouldFailOperationIfNoMatchFoundOrQueryFails(QueryDetails.StatusEnum status) {
    try (var context = new FolioExecutionContextSetter(folioExecutionContext)) {
      var operationId = UUID.randomUUID();
      var fqlQueryId = UUID.randomUUID();
      var operation = BulkOperation.builder().id(operationId).fqlQueryId(fqlQueryId).build();
      var queryDetails =
          new QueryDetails().status(status).failureReason("some reason").totalRecords(0);

      when(queryClient.getQuery(fqlQueryId, true)).thenReturn(queryDetails);
      when(queryClient.executeQuery(any(SubmitQuery.class)))
          .thenReturn(new QueryIdentifier().queryId(fqlQueryId));

      queryService.retrieveRecordsAndCheckQueryExecutionStatus(operation);

      var expectedPath = String.format(QUERY_FILENAME_TEMPLATE, operationId);

      verify(remoteFileSystemClient, times(0))
          .put(any(ByteArrayInputStream.class), eq(expectedPath));
      var operationCaptor = ArgumentCaptor.forClass(BulkOperation.class);
      await()
          .untilAsserted(
              () -> verify(bulkOperationRepository, times(2)).save(operationCaptor.capture()));
      assertThat(operationCaptor.getValue().getStatus()).isEqualTo(OperationStatusType.FAILED);
      assertThat(operationCaptor.getValue().getEndTime()).isNotNull();
    }
  }

  @Test
  void shouldCancelOperationIfQueryWasCancelled() {
    try (var context = new FolioExecutionContextSetter(folioExecutionContext)) {
      var operationId = UUID.randomUUID();
      var fqlQueryId = UUID.randomUUID();
      var operation = BulkOperation.builder().id(operationId).fqlQueryId(fqlQueryId).build();
      var queryDetails = new QueryDetails().status(QueryDetails.StatusEnum.CANCELLED);

      when(queryClient.getQuery(fqlQueryId, true)).thenReturn(queryDetails);
      when(queryClient.executeQuery(any(SubmitQuery.class)))
          .thenReturn(new QueryIdentifier().queryId(fqlQueryId));

      queryService.retrieveRecordsAndCheckQueryExecutionStatus(operation);

      var operationCaptor = ArgumentCaptor.forClass(BulkOperation.class);
      await()
          .untilAsserted(
              () -> verify(bulkOperationRepository, times(2)).save(operationCaptor.capture()));
      assertThat(operationCaptor.getValue().getStatus()).isEqualTo(CANCELLED);
    }
  }

  @Test
  void shouldReturnOperationIfQueryInProgress() {
    try (var context = new FolioExecutionContextSetter(folioExecutionContext)) {
      var operationId = UUID.randomUUID();
      var fqlQueryId = UUID.randomUUID();
      var operation =
          BulkOperation.builder()
              .id(operationId)
              .fqlQueryId(fqlQueryId)
              .status(EXECUTING_QUERY)
              .build();
      var queryDetails =
          new QueryDetails()
              .content(List.of())
              .totalRecords(1)
              .status(QueryDetails.StatusEnum.IN_PROGRESS);
      List<BulkOperationExecutionContent> contents = new ArrayList<>();

      when(queryClient.getQuery(fqlQueryId, true)).thenReturn(queryDetails);
      Assertions.assertNotNull(queryDetails.getContent());
      when(fqmContentFetcher.fetch(
              fqlQueryId,
              operation.getEntityType(),
              queryDetails.getTotalRecords(),
              contents,
              operationId))
          .thenReturn(
              new ByteArrayInputStream(
                  queryDetails.getContent().stream()
                      .map(json -> json.get("instance.jsonb").toString())
                      .collect(Collectors.joining(","))
                      .getBytes()));
      when(queryClient.executeQuery(any(SubmitQuery.class)))
          .thenReturn(new QueryIdentifier().queryId(fqlQueryId));

      var result = queryService.retrieveRecordsAndCheckQueryExecutionStatus(operation);

      assertThat(result.getStatus()).isEqualTo(RETRIEVING_RECORDS);
      assertThat(result.getLinkToTriggeringCsvFile()).isNull();
    }
  }

  @Test
  @SneakyThrows
  void shouldStartQueryOperation() {
    try (var context = new FolioExecutionContextSetter(folioExecutionContext)) {
      var queryId = UUID.randomUUID();
      var instanceJsonb = Files.readString(Path.of(INSTANCE_JSON_PATH));

      var record1 = objectMapper.createObjectNode();
      record1.set("entity", objectMapper.readTree(instanceJsonb));
      record1.put("tenantId", "diku");
      List<BulkOperationExecutionContent> contents = new ArrayList<>();

      var operation =
          BulkOperation.builder()
              .id(UUID.randomUUID())
              .status(OperationStatusType.EXECUTING_QUERY)
              .approach(org.folio.bulkops.domain.dto.ApproachType.QUERY)
              .entityType(org.folio.bulkops.domain.dto.EntityType.INSTANCE)
              .identifierType(IdentifierType.ID)
              .fqlQueryId(queryId)
              .build();
      var queryDetails =
          new QueryDetails()
              .content(
                  List.of(
                      Map.of(
                          "instance.jsonb",
                          instanceJsonb,
                          "instance.id",
                          "69640328-788e-43fc-9c3c-af39e243f3b7")))
              .status(QueryDetails.StatusEnum.SUCCESS)
              .totalRecords(1);

      when(queryClient.executeQuery(any(SubmitQuery.class)))
          .thenReturn(new QueryIdentifier().queryId(queryId));
      when(queryClient.getQuery(queryId, true)).thenReturn(queryDetails);
      when(fqmContentFetcher.fetch(
              queryId,
              EntityType.INSTANCE,
              queryDetails.getTotalRecords(),
              contents,
              operation.getId()))
          .thenReturn(new ByteArrayInputStream(record1.toString().getBytes()));

      when(bulkOperationRepository.save(any(BulkOperation.class))).thenReturn(operation);
      when(userClient.getUserById(any(String.class)))
          .thenReturn(User.builder().username("username").build());
      when(readPermissionsValidator.isBulkEditReadPermissionExists("diku", EntityType.INSTANCE))
          .thenReturn(true);
      when(remoteFileSystemClient.writer(any(String.class))).thenReturn(writer);
      var instanceStatus =
          InstanceStatus.builder().id("2a340d34-6b70-443a-bb1b-1b8d1c65d862").name("Other").build();
      when(instanceStatusesClient.getById("2a340d34-6b70-443a-bb1b-1b8d1c65d862"))
          .thenReturn(instanceStatus);
      var natureOfContentTerm =
          NatureOfContentTerm.builder()
              .id("44cd89f3-2e76-469f-a955-cc57cb9e0395")
              .name("textbook")
              .build();
      when(natureOfContentTermsClient.getById("44cd89f3-2e76-469f-a955-cc57cb9e0395"))
          .thenReturn(natureOfContentTerm);
      var instanceFormat =
          InstanceFormat.builder()
              .id("fe1b9adb-e0cf-4e05-905f-ce9986279404")
              .name("computer -- other")
              .build();
      when(instanceFormatsClient.getById("fe1b9adb-e0cf-4e05-905f-ce9986279404"))
          .thenReturn(instanceFormat);
      var instanceType =
          InstanceType.builder().id("6312d172-f0cf-40f6-b27d-9fa8feaf332f").name("text").build();
      when(instanceTypesClient.getById("6312d172-f0cf-40f6-b27d-9fa8feaf332f"))
          .thenReturn(instanceType);
      var modeOfIssuance =
          ModeOfIssuance.builder()
              .id("068b5344-e2a6-40df-9186-1829e13cd344")
              .name("serial")
              .build();
      when(modesOfIssuanceClient.getById("068b5344-e2a6-40df-9186-1829e13cd344"))
          .thenReturn(modeOfIssuance);

      queryService.retrieveRecordsAndCheckQueryExecutionStatus(operation);

      var operationCaptor = ArgumentCaptor.forClass(BulkOperation.class);
      await()
          .untilAsserted(
              () -> verify(bulkOperationRepository, times(4)).save(operationCaptor.capture()));
      assertThat(operationCaptor.getValue().getStatus())
          .isEqualTo(OperationStatusType.DATA_MODIFICATION);

      assertThat(operationCaptor.getValue().getEndTime()).isNotNull();
      assertThat(operationCaptor.getValue().getLinkToTriggeringCsvFile()).isNotNull();
      assertThat(operationCaptor.getValue().getLinkToMatchedRecordsJsonFile()).isNotNull();
      assertThat(operationCaptor.getValue().getLinkToMatchedRecordsCsvFile()).isNotNull();
    }
  }

  @Test
  @SneakyThrows
  void shouldThrowReadExceptionWhenNoPermission() {
    try (var context = new FolioExecutionContextSetter(folioExecutionContext)) {
      var queryId = UUID.randomUUID();
      var instanceJsonb = Files.readString(Path.of(INSTANCE_JSON_PATH));

      var record1 = objectMapper.createObjectNode();
      record1.set("entity", objectMapper.readTree(instanceJsonb));
      record1.put("tenantId", "diku");
      List<BulkOperationExecutionContent> contents = new ArrayList<>();
      var operation =
          BulkOperation.builder()
              .id(UUID.randomUUID())
              .status(OperationStatusType.EXECUTING_QUERY)
              .approach(org.folio.bulkops.domain.dto.ApproachType.QUERY)
              .entityType(org.folio.bulkops.domain.dto.EntityType.INSTANCE)
              .fqlQueryId(queryId)
              .build();
      var queryDetails =
          new QueryDetails()
              .content(
                  List.of(
                      Map.of(
                          "instance.jsonb",
                          instanceJsonb,
                          "instance.id",
                          "69640328-788e-43fc-9c3c-af39e243f3b7")))
              .status(QueryDetails.StatusEnum.SUCCESS)
              .totalRecords(1);

      when(queryClient.executeQuery(any(SubmitQuery.class)))
          .thenReturn(new QueryIdentifier().queryId(queryId));
      when(queryClient.getQuery(queryId, true)).thenReturn(queryDetails);
      when(fqmContentFetcher.fetch(
              queryId,
              operation.getEntityType(),
              queryDetails.getTotalRecords(),
              contents,
              operation.getId()))
          .thenReturn(new ByteArrayInputStream(record1.toString().getBytes()));
      when(bulkOperationRepository.save(any(BulkOperation.class))).thenReturn(operation);
      when(userClient.getUserById(any(String.class)))
          .thenReturn(User.builder().username("username").build());
      when(remoteFileSystemClient.writer(any(String.class))).thenReturn(writer);
      doThrow(
              new ReadPermissionException(
                  "User username does not have "
                      + "required permission to view the instance record - id="
                      + "69640328-788e-43fc-9c3c-af39e243f3b7 on the tenant diku",
                  "69640328-788e-43fc-9c3c-af39e243f3b7"))
          .when(permissionsValidator)
          .checkPermissions(any(BulkOperation.class), any(BulkOperationsEntity.class));

      queryService.retrieveRecordsAndCheckQueryExecutionStatus(operation);

      var operationCaptor = ArgumentCaptor.forClass(BulkOperation.class);
      var executionContentsCaptor = ArgumentCaptor.forClass(List.class);
      await()
          .untilAsserted(
              () -> {
                verify(errorService)
                    .saveErrorsAfterQuery(
                        executionContentsCaptor.capture(), operationCaptor.capture());
                assertThat(
                        ((BulkOperationExecutionContent)
                                executionContentsCaptor.getValue().getFirst())
                            .getErrorMessage())
                    .isEqualTo(
                        "User username does not have required permission to "
                            + "view the instance record - id=69640328-788e-43fc-9c3c-af39e243f3b7 "
                            + "on the tenant diku");
                assertThat(operationCaptor.getValue().getStatus()).isEqualTo(COMPLETED_WITH_ERRORS);
                assertThat(operationCaptor.getValue().getTotalNumOfRecords()).isEqualTo(1);
                assertThat(operationCaptor.getValue().getProcessedNumOfRecords()).isEqualTo(1);
                assertThat(operationCaptor.getValue().getMatchedNumOfErrors()).isEqualTo(1);
                assertThat(operationCaptor.getValue().getMatchedNumOfRecords()).isZero();
              });
    }
  }

  @Test
  @SneakyThrows
  void shouldThrowSrsMissingExceptionWhenNoSrs() {
    try (var context = new FolioExecutionContextSetter(folioExecutionContext)) {
      var queryId = UUID.randomUUID();
      var instanceJsonb = Files.readString(Path.of(INSTANCE_MARC_JSON_PATH));
      var record1 = objectMapper.createObjectNode();
      record1.set("entity", objectMapper.readTree(instanceJsonb));
      record1.put("tenantId", "diku");
      List<BulkOperationExecutionContent> contents = new ArrayList<>();

      var operation =
          BulkOperation.builder()
              .id(UUID.randomUUID())
              .status(OperationStatusType.EXECUTING_QUERY)
              .approach(org.folio.bulkops.domain.dto.ApproachType.QUERY)
              .entityType(EntityType.INSTANCE_MARC)
              .fqlQueryId(queryId)
              .build();
      var queryDetails =
          new QueryDetails()
              .content(
                  List.of(
                      Map.of(
                          "instance.jsonb",
                          instanceJsonb,
                          "instance.id",
                          "69640328-788e-43fc-9c3c-af39e243f3b7")))
              .status(QueryDetails.StatusEnum.SUCCESS)
              .totalRecords(1);

      when(queryClient.executeQuery(any(SubmitQuery.class)))
          .thenReturn(new QueryIdentifier().queryId(queryId));
      when(queryClient.getQuery(queryId, true)).thenReturn(queryDetails);
      when(fqmContentFetcher.fetch(
              queryId,
              operation.getEntityType(),
              queryDetails.getTotalRecords(),
              contents,
              operation.getId()))
          .thenReturn(new ByteArrayInputStream(record1.toString().getBytes()));
      when(bulkOperationRepository.save(any(BulkOperation.class))).thenReturn(operation);
      when(userClient.getUserById(any(String.class)))
          .thenReturn(User.builder().username("username").build());
      var srsRecordsNode = objectMapper.createObjectNode();
      srsRecordsNode.set("sourceRecords", objectMapper.valueToTree(List.of()));
      when(srsClient.getMarc(anyString(), anyString(), anyBoolean())).thenReturn(srsRecordsNode);
      when(remoteFileSystemClient.writer(any(String.class))).thenReturn(writer);

      queryService.retrieveRecordsAndCheckQueryExecutionStatus(operation);

      await()
          .untilAsserted(
              () -> {
                var operationCaptor = ArgumentCaptor.forClass(BulkOperation.class);
                var executionContentsCaptor = ArgumentCaptor.forClass(List.class);
                verify(errorService)
                    .saveErrorsAfterQuery(
                        executionContentsCaptor.capture(), operationCaptor.capture());
                assertThat(
                        ((BulkOperationExecutionContent)
                                executionContentsCaptor.getValue().getFirst())
                            .getErrorMessage())
                    .isEqualTo(SRS_MISSING);
                assertThat(operationCaptor.getValue().getStatus()).isEqualTo(COMPLETED_WITH_ERRORS);
                assertThat(operationCaptor.getValue().getTotalNumOfRecords()).isEqualTo(1);
                assertThat(operationCaptor.getValue().getProcessedNumOfRecords()).isEqualTo(1);
                assertThat(operationCaptor.getValue().getMatchedNumOfErrors()).isEqualTo(1);
                assertThat(operationCaptor.getValue().getMatchedNumOfRecords()).isZero();
              });
    }
  }

  @Test
  @SneakyThrows
  void shouldThrowMultipleSrsExceptionWhenMoreThanOneMarc() {
    try (var context = new FolioExecutionContextSetter(folioExecutionContext)) {
      var queryId = UUID.randomUUID();
      var instanceJsonb = Files.readString(Path.of(INSTANCE_MARC_JSON_PATH));

      var record1 = objectMapper.createObjectNode();
      record1.set("entity", objectMapper.readTree(instanceJsonb));
      record1.put("tenantId", "diku");
      List<BulkOperationExecutionContent> contents = new ArrayList<>();

      var operation =
          BulkOperation.builder()
              .id(UUID.randomUUID())
              .status(OperationStatusType.EXECUTING_QUERY)
              .approach(org.folio.bulkops.domain.dto.ApproachType.QUERY)
              .entityType(EntityType.INSTANCE_MARC)
              .fqlQueryId(queryId)
              .build();

      var queryDetails =
          new QueryDetails()
              .content(
                  List.of(
                      Map.of(
                          "instance.jsonb",
                          instanceJsonb,
                          "instance.id",
                          "69640328-788e-43fc-9c3c-af39e243f3b7")))
              .totalRecords(1)
              .status(QueryDetails.StatusEnum.SUCCESS)
              .totalRecords(1);
      String srsJson =
          """
              {
                "sourceRecords": [
                    { "recordId": "22240328-788e-43fc-9c3c-af39e243f3b7" },
                    { "recordId": "33340328-788e-43fc-9c3c-af39e243f3b7" }
                  ]
              }
              """;

      when(queryClient.executeQuery(any(SubmitQuery.class)))
          .thenReturn(new QueryIdentifier().queryId(queryId));
      when(queryClient.getQuery(queryId, true)).thenReturn(queryDetails);
      when(fqmContentFetcher.fetch(
              queryId,
              operation.getEntityType(),
              queryDetails.getTotalRecords(),
              contents,
              operation.getId()))
          .thenReturn(new ByteArrayInputStream(record1.toString().getBytes()));
      when(bulkOperationRepository.save(any(BulkOperation.class))).thenReturn(operation);
      when(userClient.getUserById(any(String.class)))
          .thenReturn(User.builder().username("username").build());
      var srsRecordsNode = objectMapper.readTree(srsJson);
      when(srsClient.getMarc(anyString(), anyString(), anyBoolean())).thenReturn(srsRecordsNode);
      when(remoteFileSystemClient.writer(any(String.class))).thenReturn(writer);

      queryService.retrieveRecordsAndCheckQueryExecutionStatus(operation);

      await()
          .untilAsserted(
              () -> {
                var operationCaptor = ArgumentCaptor.forClass(BulkOperation.class);
                var executionContentsCaptor = ArgumentCaptor.forClass(List.class);
                verify(errorService)
                    .saveErrorsAfterQuery(
                        executionContentsCaptor.capture(), operationCaptor.capture());
                assertThat(
                        ((BulkOperationExecutionContent)
                                executionContentsCaptor.getValue().getFirst())
                            .getErrorMessage())
                    .isEqualTo(
                        MULTIPLE_SRS.formatted(
                            "22240328-788e-43fc-9c3c-af39e243f3b7, "
                                + "33340328-788e-43fc-9c3c-af39e243f3b7"));
                assertThat(operationCaptor.getValue().getStatus()).isEqualTo(COMPLETED_WITH_ERRORS);
                assertThat(operationCaptor.getValue().getTotalNumOfRecords()).isEqualTo(1);
                assertThat(operationCaptor.getValue().getProcessedNumOfRecords()).isEqualTo(1);
                assertThat(operationCaptor.getValue().getMatchedNumOfErrors()).isEqualTo(1);
                assertThat(operationCaptor.getValue().getMatchedNumOfRecords()).isZero();
              });
    }
  }

  @Test
  @SneakyThrows
  void shouldThrowExceptionIfMacContentIsInvalid() {
    try (var context = new FolioExecutionContextSetter(folioExecutionContext)) {
      var queryId = UUID.randomUUID();
      var instanceJsonb = Files.readString(Path.of(INSTANCE_MARC_JSON_PATH));

      var record1 = objectMapper.createObjectNode();
      record1.set("entity", objectMapper.readTree(instanceJsonb));
      record1.put("tenantId", "diku");
      List<BulkOperationExecutionContent> contents = new ArrayList<>();

      var operation =
          BulkOperation.builder()
              .id(UUID.randomUUID())
              .status(OperationStatusType.EXECUTING_QUERY)
              .approach(org.folio.bulkops.domain.dto.ApproachType.QUERY)
              .entityType(EntityType.INSTANCE_MARC)
              .fqlQueryId(queryId)
              .build();

      var queryDetails =
          new QueryDetails()
              .content(
                  List.of(
                      Map.of(
                          "instance.jsonb",
                          instanceJsonb,
                          "instance.id",
                          "69640328-788e-43fc-9c3c-af39e243f3b7")))
              .totalRecords(1)
              .status(QueryDetails.StatusEnum.SUCCESS)
              .totalRecords(1);
      var srsJson =
          objectMapper.readTree(
              new File("src/test/resources/files/srs_response_corrupted_marc.json"));

      when(queryClient.executeQuery(any(SubmitQuery.class)))
          .thenReturn(new QueryIdentifier().queryId(queryId));
      when(queryClient.getQuery(queryId, true)).thenReturn(queryDetails);
      when(fqmContentFetcher.fetch(
              queryId,
              operation.getEntityType(),
              queryDetails.getTotalRecords(),
              contents,
              operation.getId()))
          .thenReturn(new ByteArrayInputStream(record1.toString().getBytes()));
      when(bulkOperationRepository.save(any(BulkOperation.class))).thenReturn(operation);
      when(userClient.getUserById(any(String.class)))
          .thenReturn(User.builder().username("username").build());
      when(srsClient.getMarc(anyString(), anyString(), anyBoolean())).thenReturn(srsJson);
      when(remoteFileSystemClient.writer(any(String.class))).thenReturn(writer);

      queryService.retrieveRecordsAndCheckQueryExecutionStatus(operation);

      await()
          .untilAsserted(
              () -> {
                var operationCaptor = ArgumentCaptor.forClass(BulkOperation.class);
                var executionContentsCaptor = ArgumentCaptor.forClass(List.class);
                verify(errorService)
                    .saveErrorsAfterQuery(
                        executionContentsCaptor.capture(), operationCaptor.capture());
                assertThat(
                        ((BulkOperationExecutionContent)
                                executionContentsCaptor.getValue().getFirst())
                            .getErrorMessage())
                    .isEqualTo(INVALID_MARC_MESSAGE);
                assertThat(operationCaptor.getValue().getStatus()).isEqualTo(COMPLETED_WITH_ERRORS);
                assertThat(operationCaptor.getValue().getTotalNumOfRecords()).isEqualTo(1);
                assertThat(operationCaptor.getValue().getProcessedNumOfRecords()).isEqualTo(1);
                assertThat(operationCaptor.getValue().getMatchedNumOfErrors()).isEqualTo(1);
                assertThat(operationCaptor.getValue().getMatchedNumOfRecords()).isZero();
              });
    }
  }

  @Test
  @SneakyThrows
  void shouldCollectUsedTenantsFromProcessedRecords() {
    try (var context = new FolioExecutionContextSetter(folioExecutionContext)) {

      var instanceJsonb = objectMapper.createObjectNode();
      instanceJsonb.put("id", "69640328-788e-43fc-9c3c-af39e243f3b7");
      instanceJsonb.put("title", "Test Instance");
      instanceJsonb.put("source", "FOLIO");

      // Mock two records with different tenants
      var record1 = objectMapper.createObjectNode();
      record1.set("entity", instanceJsonb);
      record1.put("tenantId", "tenantA");
      var record2 = objectMapper.createObjectNode();
      record2.set("entity", instanceJsonb);
      record2.put("tenantId", "tenantB");

      var queryDetails =
          new QueryDetails()
              .content(
                  List.of(
                      Map.of("instance.jsonb", instanceJsonb, "instance.tenant_id", "tenantA"),
                      Map.of("instance.jsonb", instanceJsonb, "instance.tenant_id", "tenantB")))
              .totalRecords(2)
              .status(QueryDetails.StatusEnum.SUCCESS);
      List<BulkOperationExecutionContent> contents = new ArrayList<>();

      var queryId = UUID.randomUUID();

      var operation =
          BulkOperation.builder()
              .id(UUID.randomUUID())
              .status(OperationStatusType.EXECUTING_QUERY)
              .approach(org.folio.bulkops.domain.dto.ApproachType.QUERY)
              .entityType(org.folio.bulkops.domain.dto.EntityType.INSTANCE)
              .fqlQueryId(queryId)
              .build();

      when(queryClient.executeQuery(any(SubmitQuery.class)))
          .thenReturn(new QueryIdentifier().queryId(queryId));
      when(queryClient.getQuery(queryId, true)).thenReturn(queryDetails);
      when(fqmContentFetcher.fetch(
              queryId,
              operation.getEntityType(),
              queryDetails.getTotalRecords(),
              contents,
              operation.getId()))
          .thenReturn(
              new ByteArrayInputStream(
                  String.join("\n", record1.toString(), record2.toString()).getBytes()));

      when(bulkOperationRepository.save(any(BulkOperation.class))).thenReturn(operation);
      when(userClient.getUserById(any(String.class)))
          .thenReturn(User.builder().username("username").build());
      when(remoteFileSystemClient.writer(any(String.class))).thenReturn(writer);
      when(readPermissionsValidator.isBulkEditReadPermissionExists(anyString(), any()))
          .thenReturn(true);

      queryService.retrieveRecordsAndCheckQueryExecutionStatus(operation);

      await()
          .untilAsserted(
              () -> {
                assertThat(operation.getUsedTenants())
                    .containsExactlyInAnyOrder("tenantA", "tenantB");
              });
    }
  }

  @Test
  void shouldSaveIdentifiersSuccessfully() {
    var operationId = UUID.randomUUID();
    var fqlQueryId = UUID.randomUUID();
    var bulkOperation = BulkOperation.builder().id(operationId).fqlQueryId(fqlQueryId).build();

    List<List<String>> ids =
        List.of(
            List.of("id1"), List.of("id2"), List.of("id1") // duplicate to test distinct
            );
    when(queryClient.getSortedIds(fqlQueryId, 0, Integer.MAX_VALUE)).thenReturn(ids);

    var expectedPath = String.format(QueryService.QUERY_FILENAME_TEMPLATE, operationId);

    when(remoteFileSystemClient.put(any(ByteArrayInputStream.class), eq(expectedPath)))
        .thenReturn("link");
    when(bulkOperationRepository.save(any(BulkOperation.class))).thenReturn(bulkOperation);

    queryService.saveIdentifiers(bulkOperation);

    verify(remoteFileSystemClient).put(any(ByteArrayInputStream.class), eq(expectedPath));
    verify(bulkOperationRepository).save(bulkOperation);
    assertThat(bulkOperation.getLinkToTriggeringCsvFile()).isEqualTo(expectedPath);
    assertThat(bulkOperation.getStatus()).isEqualTo(OperationStatusType.SAVED_IDENTIFIERS);
    assertThat(bulkOperation.getApproach())
        .isEqualTo(org.folio.bulkops.domain.dto.ApproachType.QUERY);
  }

  @Test
  void shouldFailBulkOperationOnException() {
    var operationId = UUID.randomUUID();
    var fqlQueryId = UUID.randomUUID();
    var bulkOperation = BulkOperation.builder().id(operationId).fqlQueryId(fqlQueryId).build();

    when(queryClient.getSortedIds(fqlQueryId, 0, Integer.MAX_VALUE))
        .thenThrow(new RuntimeException("Test exception"));

    queryService.saveIdentifiers(bulkOperation);

    verify(bulkOperationRepository, times(1)).save(bulkOperation);
    assertThat(bulkOperation.getStatus())
        .isEqualTo(org.folio.bulkops.domain.dto.OperationStatusType.FAILED);
    assertThat(bulkOperation.getErrorMessage()).contains("Test exception");
  }
}
