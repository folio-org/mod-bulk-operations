package org.folio.bulkops.service;

import static org.apache.commons.lang3.StringUtils.LF;
import static org.awaitility.Awaitility.await;
import static org.folio.bulkops.domain.dto.BulkOperationStep.COMMIT;
import static org.folio.bulkops.domain.dto.BulkOperationStep.EDIT;
import static org.folio.bulkops.domain.dto.OperationStatusType.APPLY_CHANGES;
import static org.folio.bulkops.domain.dto.OperationStatusType.COMPLETED;
import static org.folio.bulkops.domain.dto.OperationStatusType.DATA_MODIFICATION;
import static org.folio.bulkops.domain.dto.OperationStatusType.REVIEW_CHANGES;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyChar;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testcontainers.shaded.org.hamcrest.MatcherAssert.assertThat;
import static org.testcontainers.shaded.org.hamcrest.Matchers.anything;
import static org.testcontainers.shaded.org.hamcrest.Matchers.containsString;
import static org.testcontainers.shaded.org.hamcrest.Matchers.equalTo;
import static org.testcontainers.shaded.org.hamcrest.Matchers.hasSize;
import static org.testcontainers.shaded.org.hamcrest.Matchers.is;
import static org.testcontainers.shaded.org.hamcrest.Matchers.notNullValue;

import lombok.SneakyThrows;
import org.folio.bulkops.BaseTest;
import org.folio.bulkops.adapters.impl.holdings.HoldingsHeaderBuilder;
import org.folio.bulkops.adapters.impl.items.ItemHeaderBuilder;
import org.folio.bulkops.adapters.impl.users.UserHeaderBuilder;
import org.folio.bulkops.client.BulkEditClient;
import org.folio.bulkops.client.DataExportSpringClient;
import org.folio.bulkops.client.InstanceClient;
import org.folio.bulkops.client.RemoteFileSystemClient;
import org.folio.bulkops.domain.bean.UserGroupCollection;
import org.folio.bulkops.domain.dto.ApproachType;
import org.folio.bulkops.domain.dto.BulkOperationStart;
import org.folio.bulkops.domain.dto.BulkOperationStep;
import org.folio.bulkops.domain.dto.Cell;
import org.folio.bulkops.domain.dto.OperationStatusType;
import org.folio.bulkops.domain.bean.BriefInstance;
import org.folio.bulkops.domain.bean.HoldingsRecordsSource;
import org.folio.bulkops.domain.bean.ItemLocation;
import org.folio.bulkops.domain.bean.StateType;
import org.folio.bulkops.domain.bean.StatusType;
import org.folio.bulkops.domain.bean.User;
import org.folio.bulkops.domain.bean.UserGroup;
import org.folio.bulkops.domain.dto.EntityType;
import org.folio.bulkops.domain.dto.IdentifierType;
import org.folio.bulkops.domain.bean.Job;
import org.folio.bulkops.domain.bean.JobStatus;
import org.folio.bulkops.domain.dto.Action;
import org.folio.bulkops.domain.dto.BulkOperationRuleCollection;
import org.folio.bulkops.domain.dto.BulkOperationRuleRuleDetails;
import org.folio.bulkops.domain.dto.UpdateActionType;
import org.folio.bulkops.domain.dto.UpdateOptionType;
import org.folio.bulkops.domain.entity.BulkOperation;
import org.folio.bulkops.domain.entity.BulkOperationDataProcessing;
import org.folio.bulkops.domain.dto.BulkOperationRule;
import org.folio.bulkops.domain.entity.BulkOperationExecution;
import org.folio.bulkops.domain.entity.BulkOperationExecutionContent;
import org.folio.bulkops.domain.entity.BulkOperationProcessingContent;
import org.folio.bulkops.exception.BadRequestException;
import org.folio.bulkops.exception.BulkOperationException;
import org.folio.bulkops.exception.NotFoundException;
import org.folio.bulkops.repository.BulkOperationDataProcessingRepository;
import org.folio.bulkops.repository.BulkOperationExecutionContentRepository;
import org.folio.bulkops.repository.BulkOperationExecutionRepository;
import org.folio.bulkops.repository.BulkOperationProcessingContentRepository;
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
import org.testcontainers.shaded.org.hamcrest.MatcherAssert;
import org.testcontainers.shaded.org.hamcrest.Matchers;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import java.util.UUID;
import java.util.stream.Collectors;

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

  @MockBean
  private InstanceClient instanceClient;

  @MockBean
  private BulkOperationProcessingContentRepository processingContentRepository;

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

    var bulkOperation = bulkOperationService.uploadCsvFile(EntityType.USER, IdentifierType.BARCODE, false, null, null, file);
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

    bulkOperationService.uploadCsvFile(EntityType.USER, IdentifierType.BARCODE, true, operationId, UUID.randomUUID(), file);

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



    bulkOperationService.uploadCsvFile(EntityType.USER, IdentifierType.BARCODE, false, null, null, file);
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

    bulkOperationService.uploadCsvFile(EntityType.USER, IdentifierType.BARCODE, false, null, null, file);
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

    bulkOperationService.uploadCsvFile(EntityType.USER, IdentifierType.BARCODE, false, null, null, file);
    bulkOperationService.startBulkOperation(bulkOperationId, any(UUID.class), new BulkOperationStart().approach(ApproachType.IN_APP).step(BulkOperationStep.UPLOAD));

    var operationCaptor = ArgumentCaptor.forClass(BulkOperation.class);
    verify(bulkOperationRepository, times(4)).save(operationCaptor.capture());
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

    bulkOperationService.uploadCsvFile(EntityType.USER, IdentifierType.BARCODE, false, null, null, file);
    bulkOperationService.startBulkOperation(bulkOperationId, any(UUID.class), new BulkOperationStart().approach(ApproachType.QUERY).step(BulkOperationStep.UPLOAD));

    var operationCaptor = ArgumentCaptor.forClass(BulkOperation.class);
    verify(bulkOperationRepository, times(4)).save(operationCaptor.capture());
    assertEquals(OperationStatusType.RETRIEVING_RECORDS, operationCaptor.getAllValues().get(3).getStatus());
  }
  @ParameterizedTest
  @EnumSource(value = ApproachType.class, names = {"IN_APP", "QUERY"}, mode = EnumSource.Mode.INCLUDE)
  @SneakyThrows
  void shouldConfirmChanges() {
    var bulkOperationId = UUID.randomUUID();
    var originalPatronGroupId = "3684a786-6671-4268-8ed0-9db82ebca60b";
    var newPatronGroupId = UUID.randomUUID().toString();
    var pathToOrigin = "path/origin.json";
    var pathToModified = bulkOperationId + "/json/modified-origin.json";
    var pathToUserJson = "src/test/resources/files/user.json";

    when(bulkOperationRepository.findById(any(UUID.class)))
      .thenReturn(Optional.of(BulkOperation.builder()
        .id(bulkOperationId)
        .status(DATA_MODIFICATION)
        .entityType(EntityType.USER)
        .identifierType(IdentifierType.BARCODE)
        .linkToMatchedRecordsJsonFile(pathToOrigin)
        .linkToModifiedRecordsJsonFile(pathToModified)
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

    when(groupClient.getGroupById(newPatronGroupId)).thenReturn(new UserGroup());
    when(groupClient.getGroupById(originalPatronGroupId)).thenReturn(new UserGroup());

    when(remoteFileSystemClient.writer(any())).thenCallRealMethod();
    when(remoteFileSystemClient.newOutputStream(any())).thenCallRealMethod();

    bulkOperationService.startBulkOperation(bulkOperationId, UUID.randomUUID(), new BulkOperationStart().approach(ApproachType.QUERY).step(EDIT));

    var processingCaptor = ArgumentCaptor.forClass(BulkOperationProcessingContent.class);
    verify(processingContentRepository).save(processingCaptor.capture());
    assertThat(processingCaptor.getValue().getState(), equalTo(StateType.PROCESSED));

    var expectedPathToModifiedFile = bulkOperationId + "/json/modified-origin.json";
    var streamCaptor = ArgumentCaptor.forClass(InputStream.class);
    var pathCaptor = ArgumentCaptor.forClass(String.class);
    verify(remoteFileSystemClient).append(streamCaptor.capture(), pathCaptor.capture());
    assertThat(new String(streamCaptor.getValue().readAllBytes()), containsString(newPatronGroupId));
    assertEquals(expectedPathToModifiedFile, pathCaptor.getValue());

    var dataProcessingCaptor = ArgumentCaptor.forClass(BulkOperationDataProcessing.class);
    verify(dataProcessingRepository, times(2)).save(dataProcessingCaptor.capture());
    var capturedDataProcessingEntity = dataProcessingCaptor.getAllValues().get(1);
    assertThat(capturedDataProcessingEntity.getProcessedNumOfRecords(), is(1));
    assertThat(capturedDataProcessingEntity.getStatus(), equalTo(StatusType.COMPLETED));
    assertThat(capturedDataProcessingEntity.getEndTime(), notNullValue());

    var bulkOperationCaptor = ArgumentCaptor.forClass(BulkOperation.class);
    verify(bulkOperationRepository).save(bulkOperationCaptor.capture());
    var capturedBulkOperation = bulkOperationCaptor.getValue();
    assertThat(capturedBulkOperation.getLinkToModifiedRecordsJsonFile(), equalTo(expectedPathToModifiedFile));
    assertThat(capturedBulkOperation.getStatus(), equalTo(OperationStatusType.REVIEW_CHANGES));
  }

  @Test
  @SneakyThrows
  void shouldUpdateStatusesWhenConfirmChangesFails() {
    var bulkOperationId = UUID.randomUUID();
    var newPatronGroupId = UUID.randomUUID().toString();
    var pathToOrigin = bulkOperationId + "/origin.json";

    when(bulkOperationRepository.findById(any(UUID.class)))
      .thenReturn(Optional.of(BulkOperation.builder()
        .id(bulkOperationId)
        .entityType(EntityType.USER)
        .identifierType(IdentifierType.BARCODE)
        .linkToMatchedRecordsJsonFile(pathToOrigin)
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
      .thenThrow(new RuntimeException("Failed to read file"));

    bulkOperationService.confirm(bulkOperationId);

    var dataProcessingCaptor = ArgumentCaptor.forClass(BulkOperationDataProcessing.class);
    verify(dataProcessingRepository, times(2)).save(dataProcessingCaptor.capture());
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
  void shouldNotConfirmChangesIfBulkOperationWasNotFound() {
    when(bulkOperationRepository.findById(any(UUID.class))).thenReturn(Optional.empty());
    var operationId = UUID.randomUUID();
    assertThrows(NotFoundException.class, () -> bulkOperationService.confirm(operationId));
  }

  @Test
  void shouldNotConfirmChangesIfNoLinkToOriginFile() {
    when(bulkOperationRepository.findById(any(UUID.class))).thenReturn(Optional.of(BulkOperation.builder().build()));
    assertThrows(BulkOperationException.class, () -> bulkOperationService.confirm(UUID.randomUUID()));
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
        .entityType(EntityType.USER)
        .status(REVIEW_CHANGES)
        .identifierType(IdentifierType.BARCODE)
        .linkToMatchedRecordsJsonFile(pathToOrigin)
        .linkToMatchedRecordsCsvFile(pathToModifiedCsv)
        .linkToModifiedRecordsJsonFile(pathToModified)
        .build()));

    when(bulkOperationRepository.save(any(BulkOperation.class)))
      .thenReturn(BulkOperation.builder()
        .id(bulkOperationId)
        .entityType(EntityType.USER)
        .identifierType(IdentifierType.BARCODE)
        .linkToMatchedRecordsJsonFile(pathToOrigin)
        .linkToMatchedRecordsCsvFile(pathToModifiedCsv)
        .linkToModifiedRecordsJsonFile(pathToModified)
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
    when(remoteFileSystemClient.newOutputStream(any())).thenCallRealMethod();

    bulkOperationService.startBulkOperation(bulkOperationId, UUID.randomUUID(), new BulkOperationStart().approach(ApproachType.IN_APP).step(COMMIT));

    verify(userClient).updateUser(any(User.class), anyString());

    var streamCaptor = ArgumentCaptor.forClass(InputStream.class);
    var pathCaptor = ArgumentCaptor.forClass(String.class);
    verify(remoteFileSystemClient).append(streamCaptor.capture(), pathCaptor.capture());
    assertEquals(new String(streamCaptor.getValue().readAllBytes()),
      Files.readString(Path.of(pathToModifiedUserJson)).trim());
    assertEquals(expectedPathToResultFile, pathCaptor.getValue());

    var executionContentCaptor = ArgumentCaptor.forClass(BulkOperationExecutionContent.class);
    verify(executionContentRepository).save(executionContentCaptor.capture());
    assertThat(executionContentCaptor.getValue().getState(), equalTo(StateType.PROCESSED));

    var executionCaptor = ArgumentCaptor.forClass(BulkOperationExecution.class);
    verify(executionRepository, times(3)).save(executionCaptor.capture());
    var updatedExecution = executionCaptor.getAllValues().get(1);
    assertThat(updatedExecution.getProcessedRecords(), is(1));
    assertThat(updatedExecution.getStatus(), equalTo(StatusType.COMPLETED));

    var operationCaptor = ArgumentCaptor.forClass(BulkOperation.class);
    verify(bulkOperationRepository, times(2)).save(operationCaptor.capture());
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
        .entityType(EntityType.USER)
        .status(COMPLETED)
        .identifierType(IdentifierType.BARCODE)
        .build()));

    assertThrows(BadRequestException.class, () -> bulkOperationService.startBulkOperation(bulkOperationId, UUID.randomUUID(), new BulkOperationStart().approach(approach).step(COMMIT)));

  }

  @Test
  @SneakyThrows
  void shouldApplyChanges() {
    var bulkOperationId = UUID.randomUUID();
    var pathToOrigin = bulkOperationId + "/json/origin.json";
    var pathToModifiedCsv = bulkOperationId + "/modified-origin.csv";
    var expectedPathToResultFile = bulkOperationId + "/json/modified-origin.json";
    var pathToUserJson = "src/test/resources/files/user.json";
    var pathToModifiedUserCsv = "src/test/resources/files/modified-user.csv";

    when(bulkOperationRepository.findById(any(UUID.class)))
      .thenReturn(Optional.of(BulkOperation.builder()
        .id(bulkOperationId)
        .entityType(EntityType.USER)
          .status(DATA_MODIFICATION)
        .identifierType(IdentifierType.BARCODE)
        .linkToMatchedRecordsJsonFile(pathToOrigin)
          .linkToModifiedRecordsCsvFile(pathToModifiedCsv)
        .build()));

    when(remoteFileSystemClient.get(pathToOrigin))
      .thenReturn(new FileInputStream(pathToUserJson));

    when(remoteFileSystemClient.get(pathToModifiedCsv))
      .thenReturn(new FileInputStream(pathToModifiedUserCsv));

    when(groupClient.getGroupByQuery(String.format("group==\"%s\"", "staff"))).thenReturn(new UserGroupCollection().withUsergroups(List.of(new UserGroup())));

    when(remoteFileSystemClient.writer(any())).thenCallRealMethod();
    when(remoteFileSystemClient.newOutputStream(any())).thenCallRealMethod();

    bulkOperationService.startBulkOperation(bulkOperationId, UUID.randomUUID(), new BulkOperationStart().approach(ApproachType.MANUAL).step(EDIT));

    var operationCaptor = ArgumentCaptor.forClass(BulkOperation.class);
    verify(bulkOperationRepository, times(1)).save(operationCaptor.capture());
    var capture = operationCaptor.getAllValues().get(0);
    assertThat(capture.getStatus(), equalTo(OperationStatusType.REVIEW_CHANGES));
    assertThat(capture.getLinkToModifiedRecordsJsonFile(), equalTo(expectedPathToResultFile));
  }

  @Test
  @SneakyThrows
  void shouldNotUpdateIfEntitiesAreEqual() {
    var bulkOperationId = UUID.randomUUID();
    var pathToOrigin = bulkOperationId + "/origin.json";
    var pathToModified = bulkOperationId + "/modified-origin.json";
    var pathToUserJson = "src/test/resources/files/user.json";

    when(bulkOperationRepository.findById(any(UUID.class)))
      .thenReturn(Optional.of(BulkOperation.builder()
        .id(bulkOperationId)
        .entityType(EntityType.USER)
        .identifierType(IdentifierType.BARCODE)
        .linkToMatchedRecordsJsonFile(pathToOrigin)
        .linkToModifiedRecordsJsonFile(pathToModified)
        .build()));

    when(bulkOperationRepository.save(any(BulkOperation.class)))
      .thenReturn(BulkOperation.builder()
        .id(bulkOperationId)
        .entityType(EntityType.USER)
        .identifierType(IdentifierType.BARCODE)
        .linkToMatchedRecordsJsonFile(pathToOrigin)
        .linkToModifiedRecordsJsonFile(pathToModified)
        .build());

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

    bulkOperationService.commit(bulkOperationId);

    verify(userClient, times(0)).updateUser(any(User.class), anyString());

    verify(executionContentRepository, times(1)).save(any(BulkOperationExecutionContent.class));

    var expectedPathToResultFile = bulkOperationId + "/json/result-origin.json";
    var streamCaptor = ArgumentCaptor.forClass(InputStream.class);
    var pathCaptor = ArgumentCaptor.forClass(String.class);
    verify(remoteFileSystemClient).append(streamCaptor.capture(), pathCaptor.capture());
    assertEquals(new String(streamCaptor.getValue().readAllBytes()),
      Files.readString(Path.of(pathToUserJson)).trim());
    assertEquals(expectedPathToResultFile, pathCaptor.getValue());
  }

  @Test
  @SneakyThrows
  void shouldFailUpdateExecutionContentStatusOnFailedUpdate() {
    var bulkOperationId = UUID.randomUUID();
    var pathToOrigin = bulkOperationId + "/origin.json";
    var pathToModified = bulkOperationId + "/modified-origin.json";
    var pathToUserJson = "src/test/resources/files/user.json";
    var pathToModifiedUserJson = "src/test/resources/files/modified-user.json";

    when(bulkOperationRepository.findById(any(UUID.class)))
      .thenReturn(Optional.of(BulkOperation.builder()
        .id(bulkOperationId)
        .entityType(EntityType.USER)
        .identifierType(IdentifierType.BARCODE)
        .linkToMatchedRecordsJsonFile(pathToOrigin)
        .linkToModifiedRecordsJsonFile(pathToModified)
        .build()));

    when(bulkOperationRepository.save(any(BulkOperation.class)))
      .thenReturn(BulkOperation.builder()
        .id(bulkOperationId)
        .entityType(EntityType.USER)
        .identifierType(IdentifierType.BARCODE)
        .linkToMatchedRecordsJsonFile(pathToOrigin)
        .linkToModifiedRecordsJsonFile(pathToModified)
        .build());

    when(executionRepository.save(any(BulkOperationExecution.class)))
      .thenReturn(BulkOperationExecution.builder()
        .processedRecords(0)
        .build());

    when(remoteFileSystemClient.get(pathToOrigin))
      .thenReturn(new FileInputStream(pathToUserJson));

    when(remoteFileSystemClient.get(pathToModified))
      .thenReturn(new FileInputStream(pathToModifiedUserJson));

    when(executionContentRepository.save(any(BulkOperationExecutionContent.class)))
      .thenReturn(BulkOperationExecutionContent.builder().build());

    doThrow(new BadRequestException("Bad request")).when(userClient).updateUser(any(User.class), anyString());

    bulkOperationService.commit(bulkOperationId);

    var executionContentCaptor = ArgumentCaptor.forClass(BulkOperationExecutionContent.class);
//    verify(executionContentRepository).save(executionContentCaptor.capture());
//    assertThat(executionContentCaptor.getValue().getState(), equalTo(StateType.FAILED));

    var expectedPathToResultFile = bulkOperationId + "/json/result-origin.json";
    var streamCaptor = ArgumentCaptor.forClass(InputStream.class);
    var pathCaptor = ArgumentCaptor.forClass(String.class);
    verify(remoteFileSystemClient).append(streamCaptor.capture(), pathCaptor.capture());
    assertEquals(new String(streamCaptor.getValue().readAllBytes()),
      Files.readString(Path.of(pathToUserJson)).trim());
    assertEquals(expectedPathToResultFile, pathCaptor.getValue());
  }

  @Test
  @SneakyThrows
  void shouldUpdateStatusesWhenCommitChangesFails() {
    var bulkOperationId = UUID.randomUUID();
    var pathToOrigin = bulkOperationId + "/json/origin.json";
    var pathToModified = bulkOperationId + "/json/modified-origin.json";

    when(bulkOperationRepository.findById(any(UUID.class)))
      .thenReturn(Optional.of(BulkOperation.builder()
        .id(bulkOperationId)
        .entityType(EntityType.USER)
        .identifierType(IdentifierType.BARCODE)
        .linkToMatchedRecordsJsonFile(pathToOrigin)
        .linkToModifiedRecordsJsonFile(pathToModified)
        .build()));

    when(bulkOperationRepository.save(any(BulkOperation.class)))
      .thenReturn(BulkOperation.builder()
        .id(bulkOperationId)
        .entityType(EntityType.USER)
        .identifierType(IdentifierType.BARCODE)
        .linkToMatchedRecordsJsonFile(pathToOrigin)
        .linkToModifiedRecordsJsonFile(pathToModified)
        .build());

    when(executionRepository.save(any(BulkOperationExecution.class)))
      .thenReturn(BulkOperationExecution.builder()
        .processedRecords(0)
        .build());

    when(remoteFileSystemClient.get(pathToOrigin))
      .thenThrow(new RuntimeException("Failed to read file"));

    bulkOperationService.commit(bulkOperationId);

    var executionCaptor = ArgumentCaptor.forClass(BulkOperationExecution.class);
    verify(executionRepository, times(2)).save(executionCaptor.capture());
    var updatedExecution = executionCaptor.getAllValues().get(1);
    assertThat(updatedExecution.getProcessedRecords(), is(0));
    assertThat(updatedExecution.getStatus(), equalTo(StatusType.FAILED));

    var operationCaptor = ArgumentCaptor.forClass(BulkOperation.class);
    verify(bulkOperationRepository, times(2)).save(operationCaptor.capture());
    var firstCapture = operationCaptor.getAllValues().get(0);
    assertThat(firstCapture.getStatus(), equalTo(OperationStatusType.APPLY_CHANGES));
    var secondCapture = operationCaptor.getAllValues().get(1);
    assertThat(secondCapture.getStatus(), equalTo(OperationStatusType.FAILED));
    assertThat(secondCapture.getEndTime(), notNullValue());
  }

  @Test
  void shouldNotCommitChangesIfBulkOperationWasNotFound() {
    when(bulkOperationRepository.findById(any(UUID.class))).thenReturn(Optional.empty());
    var operationId = UUID.randomUUID();
    assertThrows(NotFoundException.class, () -> bulkOperationService.commit(operationId));
  }

  @Test
  void shouldNotCommitChangesIfNoLinkToOriginFile() {
    when(bulkOperationRepository.findById(any(UUID.class)))
      .thenReturn(Optional.of(BulkOperation.builder()
        .linkToMatchedRecordsJsonFile(null)
        .linkToModifiedRecordsJsonFile("link")
        .build()));
    assertThrows(BulkOperationException.class, () -> bulkOperationService.commit(UUID.randomUUID()));
  }

  @Test
  void shouldProcessIfNoLinkToModifiedFile() {
    when(bulkOperationRepository.findById(any(UUID.class)))
      .thenReturn(Optional.of(BulkOperation.builder()
        .linkToMatchedRecordsJsonFile("link")
        .linkToModifiedRecordsJsonFile(null)
        .build()));
    when(bulkOperationRepository.save(any(BulkOperation.class)))
      .thenReturn(BulkOperation.builder()
        .linkToMatchedRecordsJsonFile("link")
        .linkToModifiedRecordsJsonFile(null)
        .build());
    assertDoesNotThrow(() -> bulkOperationService.commit(UUID.randomUUID()));
  }
  @ParameterizedTest
  @CsvSource(value = { "users_for_preview.json,USER,UPLOAD",
    "users_for_preview.json,USER,EDIT",
    "users_for_preview.json,USER,COMMIT",
    "items_for_preview.json,ITEM,UPLOAD",
    "items_for_preview.json,ITEM,EDIT",
    "items_for_preview.json,ITEM,COMMIT",
    "holdings_for_preview.json,HOLDINGS_RECORD,UPLOAD",
    "holdings_for_preview.json,HOLDINGS_RECORD,EDIT",
    "holdings_for_preview.json,HOLDINGS_RECORD,COMMIT" }, delimiter = ',')
  @SneakyThrows
  void shouldReturnPreviewIfAvailable(String fileName, EntityType entityType, BulkOperationStep step) {
    var path = "src/test/resources/files/" + fileName;
    var operationId = UUID.randomUUID();
    var limit = 3;

    var bulkOperation = buildBulkOperation(fileName, entityType, step).withId(operationId);
    when(bulkOperationRepository.findById(operationId))
      .thenReturn(Optional.of(bulkOperation));

    when(remoteFileSystemClient.get(anyString()))
      .thenReturn(new FileInputStream(path));

    when(groupClient.getGroupById(anyString())).thenReturn(new UserGroup().withGroup("Group"));
    when(instanceClient.getById(anyString())).thenReturn(new BriefInstance().withTitle("Title"));
    when(locationClient.getLocationById(anyString())).thenReturn(new ItemLocation().withName("Location"));
    when(holdingsSourceClient.getById(anyString())).thenReturn(new HoldingsRecordsSource().withName("Source"));

    var table = bulkOperationService.getPreview(bulkOperation, step, limit);

    assertThat(table.getRows(), hasSize(limit));
    if (EntityType.USER.equals(entityType)) {
      assertThat(table.getHeader(), equalTo(UserHeaderBuilder.getHeaders()));
    } else if (EntityType.ITEM.equals(entityType)) {
      assertThat(table.getHeader(), equalTo(ItemHeaderBuilder.getHeaders()));
    } else if (EntityType.HOLDINGS_RECORD.equals(entityType)) {
      assertThat(table.getHeader(), equalTo(HoldingsHeaderBuilder.getHeaders()));
    }
  }

  @ParameterizedTest
  @EnumSource(value = OperationStatusType.class, names = { "DATA_MODIFICATION", "REVIEW_CHANGES", "COMPLETED" }, mode = EnumSource.Mode.EXCLUDE)
  @SneakyThrows
  void shouldReturnOnlyHeadersIfPreviewIsNotAvailable(OperationStatusType status) {

    var bulkOperation = BulkOperation.builder().entityType(EntityType.USER).status(status).build();

    var table = bulkOperationService.getPreview(bulkOperation, BulkOperationStep.UPLOAD, 10);
    assertEquals(0, table.getRows().size());
    Assertions.assertTrue(table.getHeader().size() > 0);
  }

  @ParameterizedTest
  @CsvSource(value = { "users_for_preview.json,USER,UPLOAD",
    "users_for_preview.json,USER,EDIT",
    "users_for_preview.json,USER,COMMIT",
    "items_for_preview.json,ITEM,UPLOAD",
    "items_for_preview.json,ITEM,EDIT",
    "items_for_preview.json,ITEM,COMMIT",
    "holdings_for_preview.json,HOLDINGS_RECORD,UPLOAD",
    "holdings_for_preview.json,HOLDINGS_RECORD,EDIT",
    "holdings_for_preview.json,HOLDINGS_RECORD,COMMIT" }, delimiter = ',')
  @SneakyThrows
  void shouldReturnCsvPreviewIfAvailable(String fileName, EntityType entityType, BulkOperationStep step) {
    var path = "src/test/resources/files/" + fileName;
    var operationId = UUID.randomUUID();

    var bulkOperation = buildBulkOperation(fileName, entityType, step).withId(operationId);
    when(bulkOperationRepository.findById(operationId))
      .thenReturn(Optional.of(bulkOperation));

    when(remoteFileSystemClient.get(anyString()))
      .thenReturn(new FileInputStream(path));

    when(groupClient.getGroupById(anyString())).thenReturn(new UserGroup().withGroup("Group"));
    when(instanceClient.getById(anyString())).thenReturn(new BriefInstance().withTitle("Title"));
    when(locationClient.getLocationById(anyString())).thenReturn(new ItemLocation().withName("Location"));
    when(holdingsSourceClient.getById(anyString())).thenReturn(new HoldingsRecordsSource().withName("Source"));

    var csvString = bulkOperationService.getCsvPreviewForBulkOperation(bulkOperation, step);

    // 6 lines expected: 1 line for headers and 5 lines for data
    MatcherAssert.assertThat(new BufferedReader(new StringReader(csvString)).lines().count(), Matchers.equalTo(6L));

    if (EntityType.USER.equals(entityType)) {
      assertThat(new Scanner(csvString).nextLine(), equalTo(UserHeaderBuilder.getHeaders().stream()
        .map(Cell::getValue)
        .collect(Collectors.joining(","))));
    } else if (EntityType.ITEM.equals(entityType)) {
      assertThat(new Scanner(csvString).nextLine(), equalTo(ItemHeaderBuilder.getHeaders().stream()
        .map(Cell::getValue)
        .collect(Collectors.joining(","))));
    } else if (EntityType.HOLDINGS_RECORD.equals(entityType)) {
      assertThat(new Scanner(csvString).nextLine(), equalTo(HoldingsHeaderBuilder.getHeaders().stream()
        .map(Cell::getValue)
        .collect(Collectors.joining(","))));
    }
  }

  @ParameterizedTest
  @EnumSource(value = OperationStatusType.class, names = { "DATA_MODIFICATION", "REVIEW_CHANGES", "COMPLETED" }, mode = EnumSource.Mode.EXCLUDE)
  @SneakyThrows
  void shouldReturnOnlyHeaderIfCsvPreviewIsNotAvailable(OperationStatusType status) {
    var operationId = UUID.randomUUID();

    var bulkOperation = BulkOperation.builder().id(UUID.randomUUID()).entityType(EntityType.USER).status(status).build();

    var actual = bulkOperationService.getCsvPreviewForBulkOperation(bulkOperation, BulkOperationStep.EDIT);
    var expected = UserHeaderBuilder.getHeaders().stream().map(Cell::getValue).collect(Collectors.joining(",")) + LF;

    assertEquals(expected, actual);
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
    switch (step) {
    case UPLOAD:
      return BulkOperation.builder()
        .entityType(entityType)
        .linkToMatchedRecordsJsonFile(fileName)
        .build();
    case EDIT:
      return BulkOperation.builder()
        .entityType(entityType)
        .linkToModifiedRecordsJsonFile(fileName)
        .build();
    case COMMIT:
      return BulkOperation.builder()
        .entityType(entityType)
        .linkToCommittedRecordsJsonFile(fileName)
        .build();
    default:
      return new BulkOperation();
    }
  }
}
