package org.folio.bulkops.service;

import lombok.SneakyThrows;
import org.awaitility.Awaitility;
import org.folio.bulkops.BaseTest;
import org.folio.bulkops.adapters.HoldingUnifiedTableHeaderBuilder;
import org.folio.bulkops.adapters.ItemUnifiedTableHeaderBuilder;
import org.folio.bulkops.adapters.UserUnifiedTableHeaderBuilder;
import org.folio.bulkops.client.BulkEditClient;
import org.folio.bulkops.client.DataExportSpringClient;
import org.folio.bulkops.client.RemoteFileSystemClient;
import org.folio.bulkops.domain.bean.BriefInstance;
import org.folio.bulkops.domain.bean.HoldingsRecordsSource;
import org.folio.bulkops.domain.bean.ItemLocation;
import org.folio.bulkops.domain.bean.Job;
import org.folio.bulkops.domain.bean.JobStatus;
import org.folio.bulkops.domain.bean.StateType;
import org.folio.bulkops.domain.bean.StatusType;
import org.folio.bulkops.domain.bean.User;
import org.folio.bulkops.domain.bean.UserGroup;
import org.folio.bulkops.domain.bean.UserGroupCollection;
import org.folio.bulkops.domain.dto.Action;
import org.folio.bulkops.domain.dto.ApproachType;
import org.folio.bulkops.domain.dto.BulkOperationRule;
import org.folio.bulkops.domain.dto.BulkOperationRuleCollection;
import org.folio.bulkops.domain.dto.BulkOperationRuleRuleDetails;
import org.folio.bulkops.domain.dto.BulkOperationStart;
import org.folio.bulkops.domain.dto.BulkOperationStep;
import org.folio.bulkops.domain.dto.EntityType;
import org.folio.bulkops.domain.dto.IdentifierType;
import org.folio.bulkops.domain.dto.OperationStatusType;
import org.folio.bulkops.domain.dto.UpdateActionType;
import org.folio.bulkops.domain.dto.UpdateOptionType;
import org.folio.bulkops.domain.entity.BulkOperation;
import org.folio.bulkops.domain.entity.BulkOperationDataProcessing;
import org.folio.bulkops.domain.entity.BulkOperationExecution;
import org.folio.bulkops.domain.entity.BulkOperationExecutionContent;
import org.folio.bulkops.exception.BadRequestException;
import org.folio.bulkops.exception.NotFoundException;
import org.folio.bulkops.repository.BulkOperationDataProcessingRepository;
import org.folio.bulkops.repository.BulkOperationExecutionContentRepository;
import org.folio.bulkops.repository.BulkOperationExecutionRepository;
import org.folio.bulkops.repository.BulkOperationRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.folio.bulkops.domain.dto.BulkOperationStep.COMMIT;
import static org.folio.bulkops.domain.dto.BulkOperationStep.EDIT;
import static org.folio.bulkops.domain.dto.EntityType.USER;
import static org.folio.bulkops.domain.dto.OperationStatusType.APPLY_CHANGES;
import static org.folio.bulkops.domain.dto.OperationStatusType.COMPLETED;
import static org.folio.bulkops.domain.dto.OperationStatusType.DATA_MODIFICATION;
import static org.folio.bulkops.domain.dto.OperationStatusType.REVIEW_CHANGES;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testcontainers.shaded.org.hamcrest.MatcherAssert.assertThat;
import static org.testcontainers.shaded.org.hamcrest.Matchers.containsString;
import static org.testcontainers.shaded.org.hamcrest.Matchers.equalTo;
import static org.testcontainers.shaded.org.hamcrest.Matchers.hasSize;
import static org.testcontainers.shaded.org.hamcrest.Matchers.is;
import static org.testcontainers.shaded.org.hamcrest.Matchers.notNullValue;

class BulkOperationServiceTest extends BaseTest {
  @Autowired
  private BulkOperationService bulkOperationService;

  @MockBean
  private BulkOperationRepository bulkOperationRepository;

  @MockBean
  private DataExportSpringClient dataExportSpringClient;

  @MockBean
  private BulkEditClient bulkEditClient;

  @MockBean
  private RuleService ruleService;

  @MockBean
  private BulkOperationDataProcessingRepository dataProcessingRepository;

  @MockBean
  private RemoteFileSystemClient remoteFileSystemClient;

  @MockBean
  private BulkOperationExecutionRepository executionRepository;

  @MockBean
  private BulkOperationExecutionContentRepository executionContentRepository;

  @Test
  @SneakyThrows
  void shouldUploadIdentifiers() {
    var file = new MockMultipartFile("file", "barcodes.csv", MediaType.TEXT_PLAIN_VALUE, new FileInputStream("src/test/resources/files/barcodes.csv").readAllBytes());

    when(bulkOperationRepository.save(any(BulkOperation.class)))
      .thenReturn(BulkOperation.builder().id(UUID.randomUUID()).build());

    var jobId = UUID.randomUUID();
    when(dataExportSpringClient.upsertJob(any(Job.class)))
      .thenReturn(Job.builder().id(jobId).status(JobStatus.SCHEDULED).build());

    when(dataExportSpringClient.getJob(jobId))
      .thenReturn(Job.builder().id(jobId).status(JobStatus.IN_PROGRESS).build());

    when(bulkEditClient.uploadFile(eq(jobId), any(MultipartFile.class)))
      .thenReturn("3");

    var bulkOperation = bulkOperationService.uploadCsvFile(USER, IdentifierType.BARCODE, false, null, null, file);
    var bulkOperationId = bulkOperation.getId();

    when(bulkOperationRepository.findById(bulkOperationId))
      .thenReturn(Optional.of(BulkOperation.builder().id(bulkOperationId).dataExportJobId(jobId).status(OperationStatusType.NEW).linkToTriggeringCsvFile("barcodes.csv").build()));

    bulkOperationService.startBulkOperation(bulkOperation.getId(), any(UUID.class), new BulkOperationStart().approach(ApproachType.IN_APP).step(BulkOperationStep.UPLOAD));

    verify(dataExportSpringClient).upsertJob(any(Job.class));
    verify(dataExportSpringClient).getJob(jobId);
    verify(bulkEditClient, times(0)).uploadFile(jobId, file);
    verify(bulkEditClient, times(0)).startJob(jobId);

    var operationCaptor = ArgumentCaptor.forClass(BulkOperation.class);
    verify(bulkOperationRepository, times(4)).save(operationCaptor.capture());
    assertEquals(OperationStatusType.NEW, operationCaptor.getAllValues().get(0).getStatus());
    // saving during upload
    assertEquals(OperationStatusType.RETRIEVING_RECORDS, operationCaptor.getAllValues().get(2).getStatus());
    // saving during start
    assertEquals(OperationStatusType.RETRIEVING_RECORDS, operationCaptor.getAllValues().get(3).getStatus());
  }

  @Test
  @SneakyThrows
  void shouldUploadManualInstances() {
    var file = new MockMultipartFile("file", "barcodes.csv", MediaType.TEXT_PLAIN_VALUE, new FileInputStream("src/test/resources/files/modified-user.csv").readAllBytes());

    var operationId = UUID.randomUUID();
    when(bulkOperationRepository.save(any(BulkOperation.class)))
      .thenReturn(BulkOperation.builder().id(operationId).build());

    when(bulkOperationRepository.findById(operationId))
      .thenReturn(Optional.of(BulkOperation.builder().id(operationId).status(DATA_MODIFICATION).build()));

    var jobId = UUID.randomUUID();
    when(remoteFileSystemClient.put(any(InputStream.class), eq(operationId + "/barcodes.csv")))
      .thenReturn("modified.csv");

    when(remoteFileSystemClient.getNumOfLines("modified.csv"))
      .thenReturn(3);

    bulkOperationService.uploadCsvFile(USER, IdentifierType.BARCODE, true, operationId, UUID.randomUUID(), file);

    var operationCaptor = ArgumentCaptor.forClass(BulkOperation.class);
    verify(bulkOperationRepository, times(1)).save(operationCaptor.capture());
    var capture = operationCaptor.getAllValues().get(0);
    assertEquals(DATA_MODIFICATION, capture.getStatus());
    assertEquals(ApproachType.MANUAL, capture.getApproach());
    assertEquals(2, capture.getTotalNumOfRecords());
    assertEquals(2, capture.getProcessedNumOfRecords());
    assertEquals(2, capture.getMatchedNumOfRecords());
  }

  @Test
  @SneakyThrows
  void shouldUploadIdentifiersAndStartJobIfJobWasNotStarted() {
    var file = new MockMultipartFile("file", "barcodes.csv", MediaType.TEXT_PLAIN_VALUE, new FileInputStream("src/test/resources/files/barcodes.csv").readAllBytes());

    var bulkOperationId = UUID.randomUUID();
    var jobId = UUID.randomUUID();

    when(bulkOperationRepository.save(any(BulkOperation.class)))
      .thenReturn(BulkOperation.builder().id(bulkOperationId).build());


    when(dataExportSpringClient.upsertJob(any(Job.class)))
      .thenReturn(Job.builder().id(jobId).status(JobStatus.SCHEDULED).build());

    when(dataExportSpringClient.getJob(jobId))
      .thenReturn(Job.builder().id(jobId).status(JobStatus.SCHEDULED).build());

    when(bulkEditClient.uploadFile(eq(jobId), any(MultipartFile.class)))
      .thenReturn("3");


    when(bulkOperationRepository.findById(bulkOperationId))
      .thenReturn(Optional.of(BulkOperation.builder().id(bulkOperationId).status(OperationStatusType.NEW).dataExportJobId(jobId).linkToTriggeringCsvFile("barcodes.csv").build()));



    bulkOperationService.uploadCsvFile(USER, IdentifierType.BARCODE, false, null, null, file);
    bulkOperationService.startBulkOperation(bulkOperationId, any(UUID.class), new BulkOperationStart().approach(ApproachType.IN_APP).step(BulkOperationStep.UPLOAD));

    verify(dataExportSpringClient).upsertJob(any(Job.class));
    verify(dataExportSpringClient).getJob(jobId);
    verify(bulkEditClient).uploadFile(eq(jobId), any());
    verify(bulkEditClient).startJob(jobId);

    var operationCaptor = ArgumentCaptor.forClass(BulkOperation.class);
    verify(bulkOperationRepository, times(4)).save(operationCaptor.capture());
    assertEquals(OperationStatusType.NEW, operationCaptor.getAllValues().get(0).getStatus());
    assertEquals(OperationStatusType.RETRIEVING_RECORDS, operationCaptor.getAllValues().get(3).getStatus());
  }

  @ParameterizedTest
  @EnumSource(value = JobStatus.class, names = { "FAILED", "SCHEDULED" }, mode = EnumSource.Mode.INCLUDE)
  @SneakyThrows
  void shouldFailOperationWhenDataExportJobFails(JobStatus jobStatus) {
    var file = new MockMultipartFile("file", "barcodes.csv", MediaType.TEXT_PLAIN_VALUE, new FileInputStream("src/test/resources/files/barcodes.csv").readAllBytes());

    var bulkOperationId = UUID.randomUUID();

    when(bulkOperationRepository.save(any(BulkOperation.class)))
      .thenReturn(BulkOperation.builder().id(bulkOperationId).build());

    var jobId = UUID.randomUUID();

    when(bulkOperationRepository.findById(any(UUID.class)))
      .thenReturn(Optional.of(BulkOperation.builder().id(bulkOperationId).dataExportJobId(jobId).status(OperationStatusType.NEW).linkToTriggeringCsvFile("barcodes.csv").build()));

    when(dataExportSpringClient.upsertJob(any(Job.class)))
      .thenReturn(Job.builder().id(jobId).status(jobStatus).build());

    when(dataExportSpringClient.getJob(jobId))
      .thenReturn(Job.builder().id(jobId).status(JobStatus.FAILED).build());

    bulkOperationService.uploadCsvFile(USER, IdentifierType.BARCODE, false, null, null, file);
    bulkOperationService.startBulkOperation(bulkOperationId, any(UUID.class), new BulkOperationStart().approach(ApproachType.IN_APP).step(BulkOperationStep.UPLOAD));

    var operationCaptor = ArgumentCaptor.forClass(BulkOperation.class);
    verify(bulkOperationRepository, times(4)).save(operationCaptor.capture());
    assertEquals(OperationStatusType.FAILED, operationCaptor.getAllValues().get(3).getStatus());
  }

  @Test
  @SneakyThrows
  void shouldFailIfDataExportJobNotFound() {
    var file = new MockMultipartFile("file", "barcodes.csv", MediaType.TEXT_PLAIN_VALUE, new FileInputStream("src/test/resources/files/barcodes.csv").readAllBytes());

    var bulkOperationId = UUID.randomUUID();

    when(bulkOperationRepository.save(any(BulkOperation.class)))
      .thenReturn(BulkOperation.builder().id(bulkOperationId).status(OperationStatusType.NEW).build());

    when(bulkOperationRepository.findById(bulkOperationId))
      .thenReturn(Optional.of(BulkOperation.builder().id(bulkOperationId).status(OperationStatusType.NEW).build()));

    var jobId = UUID.randomUUID();
    when(dataExportSpringClient.upsertJob(any(Job.class)))
      .thenReturn(Job.builder().id(jobId).status(JobStatus.SCHEDULED).build());

    when(dataExportSpringClient.getJob(jobId))
      .thenReturn(Job.builder().id(jobId).status(JobStatus.SCHEDULED).build());

    when(bulkEditClient.uploadFile(eq(jobId), any(MultipartFile.class)))
      .thenThrow(new NotFoundException("Job was not found"));

    bulkOperationService.uploadCsvFile(USER, IdentifierType.BARCODE, false, null, null, file);
    bulkOperationService.startBulkOperation(bulkOperationId, any(UUID.class), new BulkOperationStart().approach(ApproachType.IN_APP).step(BulkOperationStep.UPLOAD));

    var operationCaptor = ArgumentCaptor.forClass(BulkOperation.class);
    Awaitility.await().untilAsserted(() -> verify(bulkOperationRepository, times(4)).save(operationCaptor.capture()));
    assertEquals(OperationStatusType.FAILED, operationCaptor.getAllValues().get(3).getStatus());
  }

  @SneakyThrows
  @Test
  void shouldStartDataExportJobForQueryApproach() {
    var file = new MockMultipartFile("file", "barcodes.csv", MediaType.TEXT_PLAIN_VALUE, new FileInputStream("src/test/resources/files/barcodes.csv").readAllBytes());

    var bulkOperationId = UUID.randomUUID();

    when(bulkOperationRepository.save(any(BulkOperation.class)))
      .thenReturn(BulkOperation.builder().id(bulkOperationId).status(OperationStatusType.NEW).build());

    var jobId = UUID.randomUUID();
    when(dataExportSpringClient.upsertJob(any(Job.class)))
      .thenReturn(Job.builder().id(jobId).status(JobStatus.SCHEDULED).build());

    bulkOperationService.uploadCsvFile(USER, IdentifierType.BARCODE, false, null, null, file);
    bulkOperationService.startBulkOperation(bulkOperationId, any(UUID.class), new BulkOperationStart().approach(ApproachType.QUERY).step(BulkOperationStep.UPLOAD));

    var operationCaptor = ArgumentCaptor.forClass(BulkOperation.class);
    verify(bulkOperationRepository, times(4)).save(operationCaptor.capture());
    assertEquals(OperationStatusType.RETRIEVING_RECORDS, operationCaptor.getAllValues().get(3).getStatus());
  }
  @ParameterizedTest
  @EnumSource(value = ApproachType.class, names = {"IN_APP", "QUERY"}, mode = EnumSource.Mode.INCLUDE)
  @SneakyThrows
  void shouldConfirmChanges(ApproachType approach) {
    var bulkOperationId = UUID.randomUUID();
    var originalPatronGroupId = "3684a786-6671-4268-8ed0-9db82ebca60b";
    var newPatronGroupId = "56c86552-20ec-41d1-964a-5a2be46969e5";
    var pathToOrigin = "path/origin.json";
    var pathToModified = bulkOperationId + "/json/modified-origin.json";
    var pathToOriginalCsv = bulkOperationId + "/origin.csv";
    var pathToUserJson = "src/test/resources/files/user.json";

    when(bulkOperationRepository.findById(any(UUID.class)))
      .thenReturn(Optional.of(BulkOperation.builder()
        .id(bulkOperationId)
        .status(DATA_MODIFICATION)
        .entityType(EntityType.USER)
        .identifierType(IdentifierType.BARCODE)
        .linkToMatchedRecordsJsonFile(pathToOrigin)
        .linkToModifiedRecordsJsonFile("existing.csv")
        .linkToModifiedRecordsCsvFile("existing.json")
        .linkToMatchedRecordsCsvFile(pathToOriginalCsv)
        .processedNumOfRecords(0)
        .build()));

    when(ruleService.getRules(bulkOperationId))
      .thenReturn(new BulkOperationRuleCollection()
        .bulkOperationRules(List.of(new BulkOperationRule()
          .ruleDetails(new BulkOperationRuleRuleDetails()
            .option(UpdateOptionType.PATRON_GROUP)
            .actions(List.of(new Action()
              .type(UpdateActionType.REPLACE_WITH)
              .updated(newPatronGroupId))))))
        .totalRecords(1));

    when(dataProcessingRepository.save(any(BulkOperationDataProcessing.class)))
      .thenReturn(BulkOperationDataProcessing.builder()
        .processedNumOfRecords(0)
        .build());

    when(remoteFileSystemClient.get(pathToOrigin))
      .thenReturn(new FileInputStream(pathToUserJson));

    when(remoteFileSystemClient.get(pathToModified))
      .thenReturn(new FileInputStream(pathToUserJson));

    // 56c86552-20ec-41d1-964a-5a2be46969e5
    when(groupClient.getGroupById(newPatronGroupId)).thenReturn(new UserGroup().withGroup("original"));
    when(groupClient.getGroupById(originalPatronGroupId)).thenReturn(new UserGroup().withGroup("updated"));

    when(remoteFileSystemClient.writer(any())).thenCallRealMethod();

    bulkOperationService.startBulkOperation(bulkOperationId, UUID.randomUUID(), new BulkOperationStart().approach(approach).step(EDIT));

    var expectedPathToModifiedCsvFile = bulkOperationId + "/modified-origin.csv";
    var streamCaptor = ArgumentCaptor.forClass(InputStream.class);
    var pathCaptor = ArgumentCaptor.forClass(String.class);
    Awaitility.await().untilAsserted(() ->verify(remoteFileSystemClient, times(2)).put(streamCaptor.capture(), pathCaptor.capture()));
    assertThat(new String(streamCaptor.getAllValues().get(0).readAllBytes()), containsString(newPatronGroupId));
    assertEquals(expectedPathToModifiedCsvFile, pathCaptor.getAllValues().get(1));

    var dataProcessingCaptor = ArgumentCaptor.forClass(BulkOperationDataProcessing.class);
    Awaitility.await().untilAsserted(() -> verify(dataProcessingRepository, times(2)).save(dataProcessingCaptor.capture()));
    var capturedDataProcessingEntity = dataProcessingCaptor.getAllValues().get(1);
    assertThat(capturedDataProcessingEntity.getProcessedNumOfRecords(), is(1));
    assertThat(capturedDataProcessingEntity.getStatus(), equalTo(StatusType.COMPLETED));
    assertThat(capturedDataProcessingEntity.getEndTime(), notNullValue());

    var bulkOperationCaptor = ArgumentCaptor.forClass(BulkOperation.class);
    Awaitility.await().untilAsserted(() -> verify(bulkOperationRepository).save(bulkOperationCaptor.capture()));
    var capturedBulkOperation = bulkOperationCaptor.getValue();
    assertThat(capturedBulkOperation.getLinkToModifiedRecordsCsvFile(), equalTo(expectedPathToModifiedCsvFile));
    assertThat(capturedBulkOperation.getStatus(), equalTo(OperationStatusType.REVIEW_CHANGES));
  }

  @Test
  @SneakyThrows
  void shouldUpdateStatusesWhenConfirmChangesFails() {
    var bulkOperationId = UUID.randomUUID();
    var newPatronGroupId = UUID.randomUUID().toString();
    var pathToOrigin = bulkOperationId + "/origin.json";

    var operation = BulkOperation.builder()
      .id(bulkOperationId)
      .entityType(USER)
      .identifierType(IdentifierType.BARCODE)
      .linkToMatchedRecordsJsonFile(pathToOrigin)
      .build();

    when(bulkOperationRepository.findById(any(UUID.class)))
      .thenReturn(Optional.of(operation));

    when(ruleService.getRules(bulkOperationId))
      .thenReturn(new BulkOperationRuleCollection()
        .bulkOperationRules(List.of(new BulkOperationRule()
          .ruleDetails(new BulkOperationRuleRuleDetails()
            .option(UpdateOptionType.PATRON_GROUP)
            .actions(List.of(new Action()
              .type(UpdateActionType.REPLACE_WITH)
              .updated(newPatronGroupId))))))
        .totalRecords(1));

    when(dataProcessingRepository.save(any(BulkOperationDataProcessing.class)))
      .thenReturn(BulkOperationDataProcessing.builder()
        .processedNumOfRecords(0)
        .build());

    when(remoteFileSystemClient.get(pathToOrigin))
      .thenThrow(new RuntimeException("Failed to read file"));

    bulkOperationService.confirm(operation);

    var dataProcessingCaptor = ArgumentCaptor.forClass(BulkOperationDataProcessing.class);
    Awaitility.await().untilAsserted(() -> verify(dataProcessingRepository, times(2)).save(dataProcessingCaptor.capture()));
    var capturedDataProcessingEntity = dataProcessingCaptor.getAllValues().get(1);
    assertThat(capturedDataProcessingEntity.getStatus(), equalTo(StatusType.FAILED));
    assertThat(capturedDataProcessingEntity.getEndTime(), notNullValue());

    var bulkOperationCaptor = ArgumentCaptor.forClass(BulkOperation.class);
    verify(bulkOperationRepository).save(bulkOperationCaptor.capture());
    var capturedBulkOperation = bulkOperationCaptor.getValue();
    assertThat(capturedBulkOperation.getStatus(), equalTo(OperationStatusType.FAILED));
    assertThat(capturedBulkOperation.getEndTime(), notNullValue());
    assertThat(capturedBulkOperation.getErrorMessage(), notNullValue());
  }

  @Test
  @SneakyThrows
  void shouldCommitChanges() {

    var bulkOperationId = UUID.randomUUID();
    var pathToOrigin = bulkOperationId + "/json/origin.json";
    var pathToModified = bulkOperationId + "/json/modified-origin.json";
    var pathToModifiedCsv = bulkOperationId + "/modified-origin.csv";
    var pathToUserJson = "src/test/resources/files/user.json";
    var pathToModifiedUserJson = "src/test/resources/files/modified-user.json";

    when(bulkOperationRepository.findById(any(UUID.class)))
      .thenReturn(Optional.of(BulkOperation.builder()
        .id(bulkOperationId)
        .entityType(USER)
        .status(REVIEW_CHANGES)
        .identifierType(IdentifierType.BARCODE)
        .linkToMatchedRecordsJsonFile(pathToOrigin)
        .linkToMatchedRecordsCsvFile(pathToModifiedCsv)
        .linkToModifiedRecordsJsonFile(pathToModified)
          .committedNumOfRecords(0)
        .build()));

    when(bulkOperationRepository.save(any(BulkOperation.class)))
      .thenReturn(BulkOperation.builder()
        .id(bulkOperationId)
        .entityType(USER)
        .identifierType(IdentifierType.BARCODE)
        .linkToMatchedRecordsJsonFile(pathToOrigin)
        .linkToMatchedRecordsCsvFile(pathToModifiedCsv)
        .linkToModifiedRecordsJsonFile(pathToModified)
        .committedNumOfRecords(0)
        .build());

    when(executionRepository.save(any(BulkOperationExecution.class)))
      .thenReturn(BulkOperationExecution.builder()
        .processedRecords(0)
        .build());

    when(remoteFileSystemClient.get(pathToOrigin))
      .thenReturn(new FileInputStream(pathToUserJson));

    when(remoteFileSystemClient.get(pathToModified))
      .thenReturn(new FileInputStream(pathToModifiedUserJson));

    var expectedPathToResultFile = bulkOperationId + "/json/result-origin.json";
    when(remoteFileSystemClient.get(expectedPathToResultFile))
      .thenReturn(new FileInputStream(pathToModifiedUserJson));

    when(executionContentRepository.save(any(BulkOperationExecutionContent.class)))
      .thenReturn(BulkOperationExecutionContent.builder().build());

    when(groupClient.getGroupById("cdd8a5c8-dce7-4d7f-859a-83754b36c740")).thenReturn(new UserGroup());

    when(remoteFileSystemClient.writer(any())).thenCallRealMethod();

    bulkOperationService.startBulkOperation(bulkOperationId, UUID.randomUUID(), new BulkOperationStart().approach(ApproachType.IN_APP).step(COMMIT));

    Awaitility.await().untilAsserted(() -> verify(userClient).updateUser(any(User.class), anyString()));

    var streamCaptor = ArgumentCaptor.forClass(InputStream.class);
    var pathCaptor = ArgumentCaptor.forClass(String.class);
    Awaitility.await().untilAsserted(() -> verify(remoteFileSystemClient, times(2)).put(streamCaptor.capture(), pathCaptor.capture()));
    assertEquals(new String(streamCaptor.getAllValues().get(0).readAllBytes()),
      Files.readString(Path.of(pathToModifiedUserJson)).trim());
    assertEquals(expectedPathToResultFile, pathCaptor.getAllValues().get(0));

    var executionContentCaptor = ArgumentCaptor.forClass(BulkOperationExecutionContent.class);
    Awaitility.await().untilAsserted(() -> verify(executionContentRepository).save(executionContentCaptor.capture()));
    assertThat(executionContentCaptor.getValue().getState(), equalTo(StateType.PROCESSED));

    var executionCaptor = ArgumentCaptor.forClass(BulkOperationExecution.class);
    Awaitility.await().untilAsserted(() -> verify(executionRepository, times(2)).save(executionCaptor.capture()));
    var updatedExecution = executionCaptor.getAllValues().get(1);
    assertThat(updatedExecution.getProcessedRecords(), is(1));
    assertThat(updatedExecution.getStatus(), equalTo(StatusType.COMPLETED));

    var operationCaptor = ArgumentCaptor.forClass(BulkOperation.class);
    Awaitility.await().untilAsserted(() -> verify(bulkOperationRepository, times(2)).save(operationCaptor.capture()));
    var firstCapture = operationCaptor.getAllValues().get(0);
    assertThat(firstCapture.getStatus(), equalTo(OperationStatusType.APPLY_CHANGES));
    var secondCapture = operationCaptor.getAllValues().get(1);
    assertThat(secondCapture.getLinkToCommittedRecordsJsonFile(), equalTo(expectedPathToResultFile));
    assertThat(secondCapture.getStatus(), equalTo(OperationStatusType.COMPLETED));
    assertThat(secondCapture.getEndTime(), notNullValue());
  }

  @ParameterizedTest
  @EnumSource(value = ApproachType.class)
  void shouldNotCommitCompletedOperation(ApproachType approach) {
    var bulkOperationId = UUID.randomUUID();

    when(bulkOperationRepository.findById(any(UUID.class)))
      .thenReturn(Optional.of(BulkOperation.builder()
        .id(bulkOperationId)
        .entityType(USER)
        .status(COMPLETED)
        .identifierType(IdentifierType.BARCODE)
        .build()));

    var bulkOperation = new BulkOperationStart().approach(approach).step(COMMIT);
    assertThrows(BadRequestException.class, () -> bulkOperationService.startBulkOperation(bulkOperationId, null, bulkOperation));

  }

  @Test
  @SneakyThrows
  void shouldApplyChanges() {
    var bulkOperationId = UUID.randomUUID();
    var pathToOrigin = bulkOperationId + "/json/origin.json";
    var pathToModifiedCsv = bulkOperationId + "/modified-origin.csv";
    var pathToPreviewJson = bulkOperationId + "/json/preview-origin.json";
    var expectedPathToResultFile = bulkOperationId + "/json/modified-origin.json";
    var pathToUserJson = "src/test/resources/files/user.json";
    var pathToModifiedUserCsv = "src/test/resources/files/modified-user.csv";

    when(bulkOperationRepository.findById(any(UUID.class)))
      .thenReturn(Optional.of(BulkOperation.builder()
        .id(bulkOperationId)
        .entityType(USER)
          .status(DATA_MODIFICATION)
        .identifierType(IdentifierType.BARCODE)
        .linkToMatchedRecordsJsonFile(pathToOrigin)
          .linkToModifiedRecordsCsvFile(pathToModifiedCsv)
          .processedNumOfRecords(0)
        .build()));

    when(remoteFileSystemClient.get(pathToOrigin))
      .thenReturn(new FileInputStream(pathToUserJson));

    when(remoteFileSystemClient.get(pathToModifiedCsv))
      .thenReturn(new FileInputStream(pathToModifiedUserCsv));

    when(groupClient.getGroupByQuery(String.format("group==\"%s\"", "staff"))).thenReturn(new UserGroupCollection().withUsergroups(List.of(new UserGroup())));

    when(remoteFileSystemClient.writer(any())).thenCallRealMethod();

    bulkOperationService.startBulkOperation(bulkOperationId, UUID.randomUUID(), new BulkOperationStart().approach(ApproachType.MANUAL).step(EDIT));

    var operationCaptor = ArgumentCaptor.forClass(BulkOperation.class);
    Awaitility.await().untilAsserted(() -> verify(bulkOperationRepository, times(1)).save(operationCaptor.capture()));
    var capture = operationCaptor.getAllValues().get(0);
    assertThat(capture.getStatus(), equalTo(OperationStatusType.REVIEW_CHANGES));
    assertThat(capture.getLinkToModifiedRecordsJsonFile(), equalTo(expectedPathToResultFile));
  }

  @Test
  @SneakyThrows
  void shouldNotUpdateIfEntitiesAreEqual() {
    var bulkOperationId = UUID.randomUUID();
    var pathToOrigin = bulkOperationId + "/origin.json";
    var pathToOriginCsv = bulkOperationId + "/origin.csv";
    var pathToModified = bulkOperationId + "/modified-origin.json";
    var pathToUserJson = "src/test/resources/files/user.json";

    var operation = BulkOperation.builder()
      .id(bulkOperationId)
      .entityType(USER)
      .identifierType(IdentifierType.BARCODE)
      .linkToMatchedRecordsJsonFile(pathToOrigin)
      .linkToMatchedRecordsCsvFile(pathToOriginCsv)
      .linkToModifiedRecordsJsonFile(pathToModified)
      .build();

    when(bulkOperationRepository.findById(any(UUID.class)))
      .thenReturn(Optional.ofNullable(operation));

    when(bulkOperationRepository.save(any(BulkOperation.class)))
      .thenAnswer(invocation -> invocation.getArguments()[0]);

    when(executionRepository.save(any(BulkOperationExecution.class)))
      .thenReturn(BulkOperationExecution.builder()
        .processedRecords(0)
        .build());

    when(remoteFileSystemClient.get(pathToOrigin))
      .thenReturn(new FileInputStream(pathToUserJson));

    when(remoteFileSystemClient.get(pathToModified))
      .thenReturn(new FileInputStream(pathToUserJson));

    when(executionContentRepository.save(any(BulkOperationExecutionContent.class)))
      .thenReturn(BulkOperationExecutionContent.builder().build());

    bulkOperationService.commit(operation);

    verify(userClient, times(0)).updateUser(any(User.class), anyString());

    Awaitility.await().untilAsserted(() -> verify(executionContentRepository, times(1)).save(any(BulkOperationExecutionContent.class)));

    var expectedPathToResultFile = bulkOperationId + "/json/result-origin.json";
    var expectedPathToResultCsvFile = bulkOperationId + "/result-origin.csv";
    var pathCaptor = ArgumentCaptor.forClass(String.class);
    Awaitility.await().untilAsserted(() -> verify(remoteFileSystemClient, times(2)).writer(pathCaptor.capture()));
    assertEquals(expectedPathToResultCsvFile, pathCaptor.getAllValues().get(0));
    assertEquals(expectedPathToResultFile, pathCaptor.getAllValues().get(1));
  }

  @Test
  @SneakyThrows
  void shouldFailUpdateExecutionContentStatusOnFailedUpdate() {
    var bulkOperationId = UUID.randomUUID();
    var pathToOrigin = bulkOperationId + "/origin.json";
    var pathToOriginCsv = bulkOperationId + "/origin.csv";
    var pathToModified = bulkOperationId + "/modified-origin.json";
    var pathToUserJson = "src/test/resources/files/user.json";
    var pathToModifiedUserJson = "src/test/resources/files/modified-user.json";

    var operation = BulkOperation.builder()
      .id(bulkOperationId)
      .entityType(USER)
      .identifierType(IdentifierType.BARCODE)
      .linkToMatchedRecordsJsonFile(pathToOrigin)
      .linkToModifiedRecordsJsonFile(pathToModified)
      .linkToMatchedRecordsCsvFile(pathToOriginCsv)
      .build();

    when(bulkOperationRepository.findById(any(UUID.class)))
      .thenReturn(Optional.of(operation));

    when(bulkOperationRepository.save(any(BulkOperation.class)))
      .thenReturn(operation);

    when(executionRepository.save(any(BulkOperationExecution.class)))
      .thenReturn(BulkOperationExecution.builder()
        .processedRecords(0)
        .build());

    when(remoteFileSystemClient.get(pathToOrigin))
      .thenReturn(new FileInputStream(pathToUserJson));

    when(remoteFileSystemClient.get(pathToModified))
      .thenReturn(new FileInputStream(pathToModifiedUserJson));

    when(remoteFileSystemClient.writer(any()))
  .thenCallRealMethod();

    when(executionContentRepository.save(any(BulkOperationExecutionContent.class)))
      .thenReturn(BulkOperationExecutionContent.builder().build());

    doThrow(new BadRequestException("Bad request")).when(userClient).updateUser(any(User.class), anyString());

    bulkOperationService.commit(operation);

    var executionContentCaptor = ArgumentCaptor.forClass(BulkOperationExecutionContent.class);
    Awaitility.await().untilAsserted(() -> verify(executionContentRepository, times(1)).save(executionContentCaptor.capture()));
    assertThat(executionContentCaptor.getValue().getState(), equalTo(StateType.FAILED));

  }

  @Test
  @SneakyThrows
  void shouldUpdateStatusesWhenCommitChangesFails() {
    var bulkOperationId = UUID.randomUUID();
    var pathToOrigin = bulkOperationId + "/json/origin.json";
    var pathToModified = bulkOperationId + "/json/modified-origin.json";

    var operation = BulkOperation.builder()
      .id(bulkOperationId)
      .entityType(USER)
      .identifierType(IdentifierType.BARCODE)
      .linkToMatchedRecordsJsonFile(pathToOrigin)
      .linkToModifiedRecordsJsonFile(pathToModified)
      .build();

    when(bulkOperationRepository.findById(any(UUID.class)))
      .thenReturn(Optional.of(operation));

    when(bulkOperationRepository.save(any(BulkOperation.class)))
      .thenAnswer(args -> args.getArguments()[0]);

    when(executionRepository.save(any(BulkOperationExecution.class)))
      .thenReturn(BulkOperationExecution.builder()
        .processedRecords(0)
        .build());

    when(remoteFileSystemClient.get(pathToOrigin))
      .thenThrow(new RuntimeException("Failed to read file"));

    bulkOperationService.commit(operation);

    var executionCaptor = ArgumentCaptor.forClass(BulkOperationExecution.class);
    Awaitility.await().untilAsserted(() -> verify(executionRepository, times(2)).save(executionCaptor.capture()));
    var updatedExecution = executionCaptor.getAllValues().get(1);
    assertThat(updatedExecution.getProcessedRecords(), is(0));
    assertThat(updatedExecution.getStatus(), equalTo(StatusType.FAILED));

    var operationCaptor = ArgumentCaptor.forClass(BulkOperation.class);
    verify(bulkOperationRepository, times(2)).save(operationCaptor.capture());
    var secondCapture = operationCaptor.getAllValues().get(1);
    assertThat(secondCapture.getStatus(), equalTo(OperationStatusType.FAILED));
    assertThat(secondCapture.getEndTime(), notNullValue());
  }

  @Test
  void shouldProcessIfNoLinkToModifiedFile() {

    var operation = BulkOperation.builder()
      .id(UUID.randomUUID())
      .linkToMatchedRecordsJsonFile("link")
      .linkToModifiedRecordsJsonFile(null)
      .build();

    when(bulkOperationRepository.findById(any(UUID.class)))
      .thenReturn(Optional.of(operation));
    when(bulkOperationRepository.save(any(BulkOperation.class)))
      .thenReturn(operation);

    assertDoesNotThrow(() -> bulkOperationService.commit(operation));
  }
  @ParameterizedTest
  @CsvSource(value = { "users_preview.csv,USER,UPLOAD",
    "users_preview.csv,USER,EDIT",
    "users_preview.csv,USER,COMMIT",
    "items_preview.csv,ITEM,UPLOAD",
    "items_preview.csv,ITEM,EDIT",
    "items_preview.csv,ITEM,COMMIT",
    "holdings_preview.csv,HOLDINGS_RECORD,UPLOAD",
    "holdings_preview.csv,HOLDINGS_RECORD,EDIT",
    "holdings_preview.csv,HOLDINGS_RECORD,COMMIT"}, delimiter = ',')
  @SneakyThrows
  void shouldReturnPreviewIfAvailable(String fileName, EntityType entityType, BulkOperationStep step) {
    var path = "src/test/resources/files/" + fileName;
    var operationId = UUID.randomUUID();
    var offset = 2;
    var limit = 5;

    var bulkOperation = buildBulkOperation(fileName, entityType, step);
    bulkOperation.setId(operationId);
    when(bulkOperationRepository.findById(operationId))
      .thenReturn(Optional.of(bulkOperation));

    when(remoteFileSystemClient.get(anyString()))
      .thenReturn(new FileInputStream(path));

    when(groupClient.getGroupById(anyString())).thenReturn(new UserGroup().withGroup("Group"));
    when(instanceClient.getById(anyString())).thenReturn(new BriefInstance().withTitle("Title"));
    when(locationClient.getLocationById(anyString())).thenReturn(new ItemLocation().withName("Location"));
    when(holdingsSourceClient.getById(anyString())).thenReturn(new HoldingsRecordsSource().withName("Source"));

    var table = bulkOperationService.getPreview(bulkOperation, step, offset, limit);

    assertThat(table.getRows(), hasSize(limit - offset));
    if (USER.equals(entityType)) {
      assertThat(table.getHeader(), equalTo(UserUnifiedTableHeaderBuilder.UserHeaderBuilder.getHeaders()));
    } else if (EntityType.ITEM.equals(entityType)) {
      assertThat(table.getHeader(), equalTo(ItemUnifiedTableHeaderBuilder.ItemHeaderBuilder.getHeaders()));
    } else if (EntityType.HOLDINGS_RECORD.equals(entityType)) {
      assertThat(table.getHeader(), equalTo(HoldingUnifiedTableHeaderBuilder.HoldingsHeaderBuilder.getHeaders()));
    }
  }

  @ParameterizedTest
  @EnumSource(value = OperationStatusType.class, names = { "DATA_MODIFICATION", "REVIEW_CHANGES", "COMPLETED" }, mode = EnumSource.Mode.EXCLUDE)
  @SneakyThrows
  void shouldReturnOnlyHeadersIfPreviewIsNotAvailable(OperationStatusType status) {

    var bulkOperation = BulkOperation.builder().entityType(USER).status(status).build();

    var table = bulkOperationService.getPreview(bulkOperation, BulkOperationStep.UPLOAD, 0, 10);
    assertEquals(0, table.getRows().size());
    Assertions.assertTrue(table.getHeader().size() > 0);
  }

  @ParameterizedTest
  @EnumSource(OperationStatusType.class)
  void shouldReturnBulkOperationById(OperationStatusType statusType) {
    var operationId = UUID.randomUUID();

    when(bulkOperationRepository.findById(operationId))
      .thenReturn(Optional.of(BulkOperation.builder()
        .id(operationId)
        .status(statusType)
        .totalNumOfRecords(10)
        .processedNumOfRecords(0)
        .build()));

    when(dataProcessingRepository.findByBulkOperationId(operationId))
      .thenReturn(Optional.of(BulkOperationDataProcessing.builder()
        .status(StatusType.ACTIVE)
        .processedNumOfRecords(5)
        .build()));

    when(executionRepository.findByBulkOperationId(operationId))
      .thenReturn(Optional.of(BulkOperationExecution.builder()
        .status(StatusType.ACTIVE)
        .processedRecords(5)
        .build()));

    var operation = bulkOperationService.getOperationById(operationId);

    if (DATA_MODIFICATION.equals(operation.getStatus()) || APPLY_CHANGES.equals(operation.getStatus())) {
      assertThat(operation.getProcessedNumOfRecords(), equalTo(5));
    } else {
      assertThat(operation.getProcessedNumOfRecords(), equalTo(0));
    }
  }

  private BulkOperation buildBulkOperation(String fileName, EntityType entityType, BulkOperationStep step) {
    return switch (step) {
      case UPLOAD -> BulkOperation.builder()
        .entityType(entityType)
        .linkToMatchedRecordsCsvFile(fileName)
        .build();
      case EDIT -> BulkOperation.builder()
        .entityType(entityType)
        .linkToModifiedRecordsCsvFile(fileName)
        .build();
      case COMMIT -> BulkOperation.builder()
        .entityType(entityType)
        .linkToCommittedRecordsCsvFile(fileName)
        .build();
    };
  }
}
