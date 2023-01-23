package org.folio.bulkops.service;

import static org.folio.bulkops.domain.dto.OperationStatusType.APPLY_CHANGES;
import static org.folio.bulkops.domain.dto.OperationStatusType.DATA_MODIFICATION;
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

import lombok.SneakyThrows;
import org.folio.bulkops.BaseTest;
import org.folio.bulkops.adapters.impl.holdings.HoldingsHeaderBuilder;
import org.folio.bulkops.adapters.impl.items.ItemHeaderBuilder;
import org.folio.bulkops.adapters.impl.users.UserHeaderBuilder;
import org.folio.bulkops.client.BulkEditClient;
import org.folio.bulkops.client.DataExportSpringClient;
import org.folio.bulkops.client.InstanceClient;
import org.folio.bulkops.client.RemoteFileSystemClient;
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
import wiremock.org.hamcrest.MatcherAssert;
import wiremock.org.hamcrest.Matchers;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStream;
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

    bulkOperationService.uploadIdentifiers(EntityType.USER, IdentifierType.BARCODE, file);

    verify(dataExportSpringClient).upsertJob(any(Job.class));
    verify(dataExportSpringClient).getJob(jobId);
    verify(bulkEditClient).uploadFile(jobId, file);
    verify(bulkEditClient, times(0)).startJob(jobId);

    var operationCaptor = ArgumentCaptor.forClass(BulkOperation.class);
    verify(bulkOperationRepository, times(2)).save(operationCaptor.capture());
    assertEquals(OperationStatusType.NEW, operationCaptor.getAllValues().get(0).getStatus());
    assertEquals(OperationStatusType.RETRIEVING_RECORDS, operationCaptor.getAllValues().get(1).getStatus());
  }

  @Test
  @SneakyThrows
  void shouldUploadIdentifiersAndStartJobIfJobWasNotStarted() {
    var file = new MockMultipartFile("file", "barcodes.csv", MediaType.TEXT_PLAIN_VALUE, new FileInputStream("src/test/resources/files/barcodes.csv").readAllBytes());

    when(bulkOperationRepository.save(any(BulkOperation.class)))
      .thenReturn(BulkOperation.builder().id(UUID.randomUUID()).build());

    var jobId = UUID.randomUUID();
    when(dataExportSpringClient.upsertJob(any(Job.class)))
      .thenReturn(Job.builder().id(jobId).status(JobStatus.SCHEDULED).build());

    when(dataExportSpringClient.getJob(jobId))
      .thenReturn(Job.builder().id(jobId).status(JobStatus.SCHEDULED).build());

    when(bulkEditClient.uploadFile(eq(jobId), any(MultipartFile.class)))
      .thenReturn("3");

    bulkOperationService.uploadIdentifiers(EntityType.USER, IdentifierType.BARCODE, file);

    verify(dataExportSpringClient).upsertJob(any(Job.class));
    verify(dataExportSpringClient).getJob(jobId);
    verify(bulkEditClient).uploadFile(jobId, file);
    verify(bulkEditClient).startJob(jobId);

    var operationCaptor = ArgumentCaptor.forClass(BulkOperation.class);
    verify(bulkOperationRepository, times(2)).save(operationCaptor.capture());
    assertEquals(OperationStatusType.NEW, operationCaptor.getAllValues().get(0).getStatus());
    assertEquals(OperationStatusType.RETRIEVING_RECORDS, operationCaptor.getAllValues().get(1).getStatus());
  }

  @ParameterizedTest
  @EnumSource(value = JobStatus.class, names = { "FAILED", "SCHEDULED" }, mode = EnumSource.Mode.INCLUDE)
  @SneakyThrows
  void shouldFailOperationWhenDataExportJobFails(JobStatus jobStatus) {
    var file = new MockMultipartFile("file", "barcodes.csv", MediaType.TEXT_PLAIN_VALUE, new FileInputStream("src/test/resources/files/barcodes.csv").readAllBytes());

    when(bulkOperationRepository.save(any(BulkOperation.class)))
      .thenReturn(BulkOperation.builder().id(UUID.randomUUID()).build());

    var jobId = UUID.randomUUID();
    when(dataExportSpringClient.upsertJob(any(Job.class)))
      .thenReturn(Job.builder().id(jobId).status(jobStatus).build());

    when(dataExportSpringClient.getJob(jobId))
      .thenReturn(Job.builder().id(jobId).status(JobStatus.FAILED).build());

    bulkOperationService.uploadIdentifiers(EntityType.USER, IdentifierType.BARCODE, file);

    var operationCaptor = ArgumentCaptor.forClass(BulkOperation.class);
    verify(bulkOperationRepository, times(2)).save(operationCaptor.capture());
    assertEquals(OperationStatusType.FAILED, operationCaptor.getAllValues().get(1).getStatus());
  }

  @Test
  @SneakyThrows
  void shouldFailIfDataExportJobNotFound() {
    var file = new MockMultipartFile("file", "barcodes.csv", MediaType.TEXT_PLAIN_VALUE, new FileInputStream("src/test/resources/files/barcodes.csv").readAllBytes());

    when(bulkOperationRepository.save(any(BulkOperation.class)))
      .thenReturn(BulkOperation.builder().id(UUID.randomUUID()).build());

    var jobId = UUID.randomUUID();
    when(dataExportSpringClient.upsertJob(any(Job.class)))
      .thenReturn(Job.builder().id(jobId).status(JobStatus.SCHEDULED).build());

    when(dataExportSpringClient.getJob(jobId))
      .thenReturn(Job.builder().id(jobId).status(JobStatus.SCHEDULED).build());

    when(bulkEditClient.uploadFile(eq(jobId), any(MultipartFile.class)))
      .thenThrow(new NotFoundException("Job was not found"));

    bulkOperationService.uploadIdentifiers(EntityType.USER, IdentifierType.BARCODE, file);

    var operationCaptor = ArgumentCaptor.forClass(BulkOperation.class);
    verify(bulkOperationRepository, times(2)).save(operationCaptor.capture());
    assertEquals(OperationStatusType.FAILED, operationCaptor.getAllValues().get(1).getStatus());
  }

  @Test
  @SneakyThrows
  void shouldConfirmChanges() {
    var bulkOperationId = UUID.randomUUID();
    var newPatronGroupId = UUID.randomUUID().toString();
    var pathToOrigin = "path/origin.json";
    var pathToUserJson = "src/test/resources/files/user.json";

    when(bulkOperationRepository.findById(any(UUID.class)))
      .thenReturn(Optional.of(BulkOperation.builder()
        .id(bulkOperationId)
        .entityType(EntityType.USER)
        .identifierType(IdentifierType.BARCODE)
        .linkToOriginFile(pathToOrigin)
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

    when(groupClient.getGroupById(newPatronGroupId)).thenReturn(new UserGroup());

    bulkOperationService.confirmChanges(bulkOperationId);

    var processingCaptor = ArgumentCaptor.forClass(BulkOperationProcessingContent.class);
    verify(processingContentRepository).save(processingCaptor.capture());
    assertThat(processingCaptor.getValue().getState(), equalTo(StateType.PROCESSED));

    var expectedPathToModifiedFile = bulkOperationId + "/modified-origin.json";
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
    assertThat(capturedBulkOperation.getLinkToModifiedFile(), equalTo(expectedPathToModifiedFile));
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
        .linkToOriginFile(pathToOrigin)
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

    bulkOperationService.confirmChanges(bulkOperationId);

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
    assertThrows(NotFoundException.class, () -> bulkOperationService.confirmChanges(operationId));
  }

  @Test
  void shouldNotConfirmChangesIfNoLinkToOriginFile() {
    when(bulkOperationRepository.findById(any(UUID.class))).thenReturn(Optional.of(BulkOperation.builder().build()));
    assertThrows(BulkOperationException.class, () -> bulkOperationService.confirmChanges(UUID.randomUUID()));
  }

  @Test
  @SneakyThrows
  void shouldCommitChanges() {
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
        .linkToOriginFile(pathToOrigin)
        .linkToModifiedFile(pathToModified)
        .build()));

    when(bulkOperationRepository.save(any(BulkOperation.class)))
      .thenReturn(BulkOperation.builder()
        .id(bulkOperationId)
        .entityType(EntityType.USER)
        .identifierType(IdentifierType.BARCODE)
        .linkToOriginFile(pathToOrigin)
        .linkToModifiedFile(pathToModified)
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

    bulkOperationService.commitChanges(bulkOperationId);

    verify(userClient).updateUser(any(User.class), anyString());

    var expectedPathToResultFile = bulkOperationId + "/result-origin.json";
    var streamCaptor = ArgumentCaptor.forClass(InputStream.class);
    var pathCaptor = ArgumentCaptor.forClass(String.class);
    verify(remoteFileSystemClient).append(streamCaptor.capture(), pathCaptor.capture());
    assertEquals(new String(streamCaptor.getValue().readAllBytes()),
      Files.readString(Path.of(pathToModifiedUserJson)));
    assertEquals(expectedPathToResultFile, pathCaptor.getValue());

    var executionContentCaptor = ArgumentCaptor.forClass(BulkOperationExecutionContent.class);
    verify(executionContentRepository).save(executionContentCaptor.capture());
    assertThat(executionContentCaptor.getValue().getState(), equalTo(StateType.PROCESSED));

    var executionCaptor = ArgumentCaptor.forClass(BulkOperationExecution.class);
    verify(executionRepository, times(2)).save(executionCaptor.capture());
    var updatedExecution = executionCaptor.getAllValues().get(1);
    assertThat(updatedExecution.getProcessedRecords(), is(1));
    assertThat(updatedExecution.getStatus(), equalTo(StatusType.COMPLETED));

    var operationCaptor = ArgumentCaptor.forClass(BulkOperation.class);
    verify(bulkOperationRepository, times(2)).save(operationCaptor.capture());
    var firstCapture = operationCaptor.getAllValues().get(0);
    assertThat(firstCapture.getStatus(), equalTo(OperationStatusType.APPLY_CHANGES));
    var secondCapture = operationCaptor.getAllValues().get(1);
    assertThat(secondCapture.getLinkToResultFile(), equalTo(expectedPathToResultFile));
    assertThat(secondCapture.getStatus(), equalTo(OperationStatusType.COMPLETED));
    assertThat(secondCapture.getEndTime(), notNullValue());
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
        .linkToOriginFile(pathToOrigin)
        .linkToModifiedFile(pathToModified)
        .build()));

    when(bulkOperationRepository.save(any(BulkOperation.class)))
      .thenReturn(BulkOperation.builder()
        .id(bulkOperationId)
        .entityType(EntityType.USER)
        .identifierType(IdentifierType.BARCODE)
        .linkToOriginFile(pathToOrigin)
        .linkToModifiedFile(pathToModified)
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

    bulkOperationService.commitChanges(bulkOperationId);

    verify(userClient, times(0)).updateUser(any(User.class), anyString());

    verify(executionContentRepository, times(0)).save(any(BulkOperationExecutionContent.class));

    var expectedPathToResultFile = bulkOperationId + "/result-origin.json";
    var streamCaptor = ArgumentCaptor.forClass(InputStream.class);
    var pathCaptor = ArgumentCaptor.forClass(String.class);
    verify(remoteFileSystemClient).append(streamCaptor.capture(), pathCaptor.capture());
    assertEquals(new String(streamCaptor.getValue().readAllBytes()),
      Files.readString(Path.of(pathToUserJson)));
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
        .linkToOriginFile(pathToOrigin)
        .linkToModifiedFile(pathToModified)
        .build()));

    when(bulkOperationRepository.save(any(BulkOperation.class)))
      .thenReturn(BulkOperation.builder()
        .id(bulkOperationId)
        .entityType(EntityType.USER)
        .identifierType(IdentifierType.BARCODE)
        .linkToOriginFile(pathToOrigin)
        .linkToModifiedFile(pathToModified)
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

    bulkOperationService.commitChanges(bulkOperationId);

    var executionContentCaptor = ArgumentCaptor.forClass(BulkOperationExecutionContent.class);
    verify(executionContentRepository).save(executionContentCaptor.capture());
    assertThat(executionContentCaptor.getValue().getState(), equalTo(StateType.FAILED));

    var expectedPathToResultFile = bulkOperationId + "/result-origin.json";
    var streamCaptor = ArgumentCaptor.forClass(InputStream.class);
    var pathCaptor = ArgumentCaptor.forClass(String.class);
    verify(remoteFileSystemClient).append(streamCaptor.capture(), pathCaptor.capture());
    assertEquals(new String(streamCaptor.getValue().readAllBytes()),
      Files.readString(Path.of(pathToUserJson)));
    assertEquals(expectedPathToResultFile, pathCaptor.getValue());
  }

  @Test
  @SneakyThrows
  void shouldUpdateStatusesWhenCommitChangesFails() {
    var bulkOperationId = UUID.randomUUID();
    var pathToOrigin = bulkOperationId + "/origin.json";
    var pathToModified = bulkOperationId + "/modified-origin.json";

    when(bulkOperationRepository.findById(any(UUID.class)))
      .thenReturn(Optional.of(BulkOperation.builder()
        .id(bulkOperationId)
        .entityType(EntityType.USER)
        .identifierType(IdentifierType.BARCODE)
        .linkToOriginFile(pathToOrigin)
        .linkToModifiedFile(pathToModified)
        .build()));

    when(bulkOperationRepository.save(any(BulkOperation.class)))
      .thenReturn(BulkOperation.builder()
        .id(bulkOperationId)
        .entityType(EntityType.USER)
        .identifierType(IdentifierType.BARCODE)
        .linkToOriginFile(pathToOrigin)
        .linkToModifiedFile(pathToModified)
        .build());

    when(executionRepository.save(any(BulkOperationExecution.class)))
      .thenReturn(BulkOperationExecution.builder()
        .processedRecords(0)
        .build());

    when(remoteFileSystemClient.get(pathToOrigin))
      .thenThrow(new RuntimeException("Failed to read file"));

    bulkOperationService.commitChanges(bulkOperationId);

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
    assertThrows(NotFoundException.class, () -> bulkOperationService.commitChanges(operationId));
  }

  @ParameterizedTest
  @CsvSource(value = { ",link", "link," }, delimiter = ',')
  void shouldNotCommitChangesIfNoLinkToOriginOrModifiedFile(String linkToOrigin, String linkToModified) {
    when(bulkOperationRepository.findById(any(UUID.class)))
      .thenReturn(Optional.of(BulkOperation.builder()
        .linkToOriginFile(linkToOrigin)
        .linkToModifiedFile(linkToModified)
        .build()));
    assertThrows(BulkOperationException.class, () -> bulkOperationService.commitChanges(UUID.randomUUID()));
  }

  @ParameterizedTest
  @CsvSource(value = { "users_for_preview.json,USER,DATA_MODIFICATION",
    "users_for_preview.json,USER,REVIEW_CHANGES",
    "users_for_preview.json,USER,COMPLETED",
    "items_for_preview.json,ITEM,DATA_MODIFICATION",
    "items_for_preview.json,ITEM,REVIEW_CHANGES",
    "items_for_preview.json,ITEM,COMPLETED",
    "holdings_for_preview.json,HOLDING,DATA_MODIFICATION",
    "holdings_for_preview.json,HOLDING,REVIEW_CHANGES",
    "holdings_for_preview.json,HOLDING,COMPLETED" }, delimiter = ',')
  @SneakyThrows
  void shouldReturnPreviewIfAvailable(String fileName, EntityType entityType, OperationStatusType status) {
    var path = "src/test/resources/files/" + fileName;
    var operationId = UUID.randomUUID();
    var limit = 3;

    var bulkOperation = buildBulkOperation(fileName, entityType, status).withId(operationId);
    when(bulkOperationRepository.findById(operationId))
      .thenReturn(Optional.of(bulkOperation));

    when(remoteFileSystemClient.get(anyString()))
      .thenReturn(new FileInputStream(path));

    when(groupClient.getGroupById(anyString())).thenReturn(new UserGroup().withGroup("Group"));
    when(instanceClient.getById(anyString())).thenReturn(new BriefInstance().withTitle("Title"));
    when(locationClient.getLocationById(anyString())).thenReturn(new ItemLocation().withName("Location"));
    when(holdingsSourceClient.getById(anyString())).thenReturn(new HoldingsRecordsSource().withName("Source"));

    var table = bulkOperationService.getPreview(operationId, limit);

    assertThat(table.getRows(), hasSize(limit));
    if (EntityType.USER.equals(entityType)) {
      assertThat(table.getHeader(), equalTo(UserHeaderBuilder.getHeaders()));
    } else if (EntityType.ITEM.equals(entityType)) {
      assertThat(table.getHeader(), equalTo(ItemHeaderBuilder.getHeaders()));
    } else if (EntityType.HOLDING.equals(entityType)) {
      assertThat(table.getHeader(), equalTo(HoldingsHeaderBuilder.getHeaders()));
    }
  }

  @ParameterizedTest
  @EnumSource(value = OperationStatusType.class, names = { "DATA_MODIFICATION", "REVIEW_CHANGES", "COMPLETED" }, mode = EnumSource.Mode.EXCLUDE)
  @SneakyThrows
  void shouldExitExceptionallyIfPreviewIsNotAvailable(OperationStatusType status) {
    var operationId = UUID.randomUUID();

    when(bulkOperationRepository.findById(operationId))
      .thenReturn(Optional.of(BulkOperation.builder().entityType(EntityType.USER).status(status).build()));

    assertThrows(NotFoundException.class, () -> bulkOperationService.getPreview(operationId, 10));
  }

  @ParameterizedTest
  @CsvSource(value = { "users_for_preview.json,USER,DATA_MODIFICATION",
    "users_for_preview.json,USER,REVIEW_CHANGES",
    "users_for_preview.json,USER,COMPLETED",
    "items_for_preview.json,ITEM,DATA_MODIFICATION",
    "items_for_preview.json,ITEM,REVIEW_CHANGES",
    "items_for_preview.json,ITEM,COMPLETED",
    "holdings_for_preview.json,HOLDING,DATA_MODIFICATION",
    "holdings_for_preview.json,HOLDING,REVIEW_CHANGES",
    "holdings_for_preview.json,HOLDING,COMPLETED" }, delimiter = ',')
  @SneakyThrows
  void shouldReturnCsvPreviewIfAvailable(String fileName, EntityType entityType, OperationStatusType status) {
    var path = "src/test/resources/files/" + fileName;
    var operationId = UUID.randomUUID();

    var bulkOperation = buildBulkOperation(fileName, entityType, status).withId(operationId);
    when(bulkOperationRepository.findById(operationId))
      .thenReturn(Optional.of(bulkOperation));

    when(remoteFileSystemClient.get(anyString()))
      .thenReturn(new FileInputStream(path));

    when(groupClient.getGroupById(anyString())).thenReturn(new UserGroup().withGroup("Group"));
    when(instanceClient.getById(anyString())).thenReturn(new BriefInstance().withTitle("Title"));
    when(locationClient.getLocationById(anyString())).thenReturn(new ItemLocation().withName("Location"));
    when(holdingsSourceClient.getById(anyString())).thenReturn(new HoldingsRecordsSource().withName("Source"));

    var csvString = bulkOperationService.getCsvPreviewByBulkOperationId(operationId);

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
    } else if (EntityType.HOLDING.equals(entityType)) {
      assertThat(new Scanner(csvString).nextLine(), equalTo(HoldingsHeaderBuilder.getHeaders().stream()
        .map(Cell::getValue)
        .collect(Collectors.joining(","))));
    }
  }

  @ParameterizedTest
  @EnumSource(value = OperationStatusType.class, names = { "DATA_MODIFICATION", "REVIEW_CHANGES", "COMPLETED" }, mode = EnumSource.Mode.EXCLUDE)
  @SneakyThrows
  void shouldExitExceptionallyIfCsvPreviewIsNotAvailable(OperationStatusType status) {
    var operationId = UUID.randomUUID();

    when(bulkOperationRepository.findById(operationId))
      .thenReturn(Optional.of(BulkOperation.builder().entityType(EntityType.USER).status(status).build()));

    assertThrows(NotFoundException.class, () -> bulkOperationService.getCsvPreviewByBulkOperationId(operationId));
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

  private BulkOperation buildBulkOperation(String fileName, EntityType entityType, OperationStatusType status) {
    switch (status) {
    case DATA_MODIFICATION:
      return BulkOperation.builder()
        .entityType(entityType)
        .status(status)
        .linkToOriginFile(fileName)
        .build();
    case REVIEW_CHANGES:
      return BulkOperation.builder()
        .entityType(entityType)
        .status(status)
        .linkToModifiedFile(fileName)
        .build();
    case COMPLETED:
      return BulkOperation.builder()
        .entityType(entityType)
        .status(status)
        .linkToResultFile(fileName)
        .build();
    default:
      return new BulkOperation();
    }
  }
}
