package org.folio.bulkops.service;

import static java.lang.String.format;
import static java.util.Objects.nonNull;
import static org.folio.bulkops.domain.dto.EntityType.HOLDINGS_RECORD;
import static org.folio.bulkops.domain.dto.EntityType.INSTANCE_MARC;
import static org.folio.bulkops.domain.dto.EntityType.ITEM;
import static org.folio.bulkops.domain.dto.OperationStatusType.APPLY_MARC_CHANGES;
import static org.folio.bulkops.domain.dto.OperationStatusType.EXECUTING_QUERY;
import static org.folio.bulkops.domain.dto.OperationStatusType.SAVED_IDENTIFIERS;
import static org.folio.bulkops.domain.dto.BulkOperationStep.COMMIT;
import static org.folio.bulkops.domain.dto.BulkOperationStep.EDIT;
import static org.folio.bulkops.domain.dto.EntityType.USER;
import static org.folio.bulkops.domain.dto.OperationStatusType.APPLY_CHANGES;
import static org.folio.bulkops.domain.dto.OperationStatusType.COMPLETED;
import static org.folio.bulkops.domain.dto.OperationStatusType.DATA_MODIFICATION;
import static org.folio.bulkops.domain.dto.OperationStatusType.REVIEW_CHANGES;
import static org.folio.bulkops.service.BulkOperationService.FILE_UPLOADING_FAILED;
import static org.folio.bulkops.service.BulkOperationService.MSG_BULK_EDIT_SUPPORTED_FOR_MARC_ONLY;
import static org.folio.bulkops.service.BulkOperationService.TMP_MATCHED_JSON_PATH_TEMPLATE;
import static org.folio.bulkops.util.Constants.APPLY_TO_ITEMS;
import static org.folio.bulkops.util.Constants.ERROR_COMMITTING_FILE_NAME_PREFIX;
import static org.folio.bulkops.util.Constants.ERROR_MATCHING_FILE_NAME_PREFIX;
import static org.folio.bulkops.util.Constants.MSG_NO_CHANGE_REQUIRED;
import static org.folio.bulkops.util.ErrorCode.ERROR_MESSAGE_PATTERN;
import static org.folio.bulkops.util.ErrorCode.ERROR_NOT_CONFIRM_CHANGES_S3_ISSUE;
import static org.folio.bulkops.util.ErrorCode.ERROR_UPLOAD_IDENTIFIERS_S3_ISSUE;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testcontainers.shaded.org.hamcrest.MatcherAssert.assertThat;
import static org.testcontainers.shaded.org.hamcrest.Matchers.containsString;
import static org.testcontainers.shaded.org.hamcrest.Matchers.equalTo;
import static org.testcontainers.shaded.org.hamcrest.Matchers.hasSize;
import static org.testcontainers.shaded.org.hamcrest.Matchers.is;
import static org.testcontainers.shaded.org.hamcrest.Matchers.notNullValue;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.assertj.core.api.Assertions;
import org.awaitility.Awaitility;
import org.folio.bulkops.BaseTest;
import org.folio.bulkops.client.BulkEditClient;
import org.folio.bulkops.client.DataExportSpringClient;
import org.folio.bulkops.client.RemoteFileSystemClient;
import org.folio.bulkops.domain.bean.ExtendedHoldingsRecord;
import org.folio.bulkops.domain.bean.ExtendedItem;
import org.folio.bulkops.domain.bean.HoldingsRecord;
import org.folio.bulkops.domain.bean.Item;
import org.folio.bulkops.domain.bean.ItemCollection;
import org.folio.bulkops.domain.bean.ItemLocation;
import org.folio.bulkops.domain.bean.Job;
import org.folio.bulkops.domain.bean.JobStatus;
import org.folio.bulkops.domain.bean.StateType;
import org.folio.bulkops.domain.bean.StatusType;
import org.folio.bulkops.domain.bean.User;
import org.folio.bulkops.domain.bean.UserGroup;
import org.folio.bulkops.domain.bean.UserGroupCollection;
import org.folio.bulkops.domain.converter.BulkOperationsEntityCsvWriter;
import org.folio.bulkops.domain.dto.Action;
import org.folio.bulkops.domain.dto.ApproachType;
import org.folio.bulkops.domain.dto.BulkOperationMarcRuleCollection;
import org.folio.bulkops.domain.dto.BulkOperationRule;
import org.folio.bulkops.domain.dto.BulkOperationRuleCollection;
import org.folio.bulkops.domain.dto.BulkOperationRuleRuleDetails;
import org.folio.bulkops.domain.dto.BulkOperationStart;
import org.folio.bulkops.domain.dto.BulkOperationStep;
import org.folio.bulkops.domain.dto.DataImportJobExecution;
import org.folio.bulkops.domain.dto.DataImportProgress;
import org.folio.bulkops.domain.dto.DataImportStatus;
import org.folio.bulkops.domain.dto.EntityType;
import org.folio.bulkops.domain.dto.ErrorType;
import org.folio.bulkops.domain.dto.IdentifierType;
import org.folio.bulkops.domain.dto.MarcAction;
import org.folio.bulkops.domain.dto.OperationStatusType;
import org.folio.bulkops.domain.dto.Parameter;
import org.folio.bulkops.domain.dto.UpdateActionType;
import org.folio.bulkops.domain.dto.UpdateOptionType;
import org.folio.bulkops.domain.entity.BulkOperation;
import org.folio.bulkops.domain.entity.BulkOperationDataProcessing;
import org.folio.bulkops.domain.entity.BulkOperationExecution;
import org.folio.bulkops.domain.entity.BulkOperationExecutionContent;
import org.folio.bulkops.domain.dto.BulkOperationMarcRule;
import org.folio.bulkops.exception.BadRequestException;
import org.folio.bulkops.exception.IllegalOperationStateException;
import org.folio.bulkops.exception.NotFoundException;
import org.folio.bulkops.processor.marc.MarcInstanceDataProcessor;
import org.folio.bulkops.processor.permissions.check.PermissionsValidator;
import org.folio.bulkops.repository.BulkOperationDataProcessingRepository;
import org.folio.bulkops.repository.BulkOperationExecutionContentRepository;
import org.folio.bulkops.repository.BulkOperationExecutionRepository;
import org.folio.bulkops.repository.BulkOperationRepository;
import org.folio.bulkops.util.MarcCsvHelper;
import org.folio.s3.client.FolioS3Client;
import org.folio.s3.client.RemoteStorageWriter;
import org.folio.s3.exception.S3ClientException;
import org.folio.spring.scope.FolioExecutionContextSetter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.marc4j.marc.Record;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.multipart.MultipartFile;


class BulkOperationServiceTest extends BaseTest {
  @Autowired
  private BulkOperationService bulkOperationService;

   @MockitoBean
  private BulkOperationRepository bulkOperationRepository;

   @MockitoBean
  private DataExportSpringClient dataExportSpringClient;

   @MockitoBean
  private BulkEditClient bulkEditClient;

   @MockitoBean
  private RuleService ruleService;

   @MockitoBean
  private BulkOperationDataProcessingRepository dataProcessingRepository;

   @MockitoBean
  private RemoteFileSystemClient remoteFileSystemClient;

   @MockitoBean
  private FolioS3Client remoteFolioS3Client;

   @MockitoBean
  private BulkOperationExecutionRepository executionRepository;

   @MockitoBean
  private BulkOperationExecutionContentRepository executionContentRepository;

   @MockitoBean
  private ErrorService errorService;

   @MockitoBean
  private ItemReferenceService itemReferenceService;

   @MockitoBean
  private QueryService queryService;

   @MockitoBean
  private ConsortiaService consortiaService;

   @MockitoBean
  private PermissionsValidator permissionsValidator;

   @MockitoBean
  private MetadataProviderService metadataProviderService;

   @MockitoBean
  private SrsService srsService;

   @MockitoBean
  private MarcCsvHelper marcCsvHelper;

   @MockitoBean
  private MarcInstanceDataProcessor marcInstanceDataProcessor;

   @MockitoBean
  private MarcUpdateService marcUpdateService;

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
  void shouldPopulateErrorToBulkOperationIfS3IssuesForUploadIdentifiersInApp() {
    var file = new MockMultipartFile("file", "barcodes.csv", MediaType.TEXT_PLAIN_VALUE, new FileInputStream("src/test/resources/files/barcodes.csv").readAllBytes());
    var jobId = UUID.randomUUID();
    var bulkOperationId = UUID.randomUUID();

    when(bulkOperationRepository.save(any(BulkOperation.class)))
      .thenReturn(BulkOperation.builder().id(bulkOperationId).build());

    when(remoteFileSystemClient.put(any(), any()))
      .thenThrow(new S3ClientException("error"));

    when(errorService.uploadErrorsToStorage(bulkOperationId, ERROR_MATCHING_FILE_NAME_PREFIX,ERROR_UPLOAD_IDENTIFIERS_S3_ISSUE + " : error"))
      .thenReturn("/linkToMatchingErrorsFile.csv");

    bulkOperationService.uploadCsvFile(USER, IdentifierType.BARCODE, false, null, null, file);

    when(bulkOperationRepository.findById(bulkOperationId))
      .thenReturn(Optional.of(BulkOperation.builder().id(bulkOperationId).dataExportJobId(jobId).status(OperationStatusType.NEW).linkToTriggeringCsvFile("barcodes.csv").build()));

    var operationCaptor = ArgumentCaptor.forClass(BulkOperation.class);
    verify(bulkOperationRepository, times(2)).save(operationCaptor.capture());
    var capturedBulkOperation = operationCaptor.getValue();
    assertThat(capturedBulkOperation.getStatus(), equalTo(OperationStatusType.FAILED));
    assertThat(capturedBulkOperation.getErrorMessage(),equalTo(format(ERROR_MESSAGE_PATTERN, ERROR_UPLOAD_IDENTIFIERS_S3_ISSUE, "error")));
    assertThat(capturedBulkOperation.getLinkToMatchedRecordsErrorsCsvFile(), equalTo("/linkToMatchingErrorsFile.csv"));
  }

  @Test
  @SneakyThrows
  void shouldUploadManualInstances() {
    var file = new MockMultipartFile("file", "barcodes.csv", MediaType.TEXT_PLAIN_VALUE, new FileInputStream("src/test/resources/files/modified-user.csv").readAllBytes());

    var operationId = UUID.randomUUID();
    when(bulkOperationRepository.save(any(BulkOperation.class)))
      .thenReturn(BulkOperation.builder().id(operationId).build());

    when(bulkOperationRepository.findById(operationId))
      .thenReturn(Optional.of(BulkOperation.builder().id(operationId).linkToTriggeringCsvFile("path/barcodes.csv").status(DATA_MODIFICATION).build()));

    var linkToPreviewFile = operationId + "/" + LocalDate.now() + "-Updates-Preview-CSV-barcodes.csv";
    when(remoteFileSystemClient.put(any(InputStream.class), eq(linkToPreviewFile)))
      .thenReturn(linkToPreviewFile);

    when(remoteFileSystemClient.getNumOfLines(linkToPreviewFile))
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
  void shouldPopulateErrorToBulkOperationIfS3IssuesForUploadIdentifiersManual() {
    var file = new MockMultipartFile("file", "barcodes.csv", MediaType.TEXT_PLAIN_VALUE, new FileInputStream("src/test/resources/files/barcodes.csv").readAllBytes());
    var bulkOperationId = UUID.randomUUID();
    var jobId = UUID.randomUUID();

    when(bulkOperationRepository.save(any(BulkOperation.class)))
      .thenReturn(BulkOperation.builder().id(bulkOperationId).build());
    when(remoteFileSystemClient.put(any(), any()))
      .thenThrow(new S3ClientException("error"));
    when(bulkOperationRepository.findById(bulkOperationId))
      .thenReturn(Optional.of(BulkOperation.builder().id(bulkOperationId).dataExportJobId(jobId).status(OperationStatusType.NEW).linkToTriggeringCsvFile("barcodes.csv").build()));

    bulkOperationService.uploadCsvFile(USER, IdentifierType.BARCODE, true, bulkOperationId, null, file);

    var operationCaptor = ArgumentCaptor.forClass(BulkOperation.class);
    verify(bulkOperationRepository, times(1)).save(operationCaptor.capture());
    var capturedBulkOperation = operationCaptor.getValue();
    assertThat(capturedBulkOperation.getStatus(), equalTo(OperationStatusType.FAILED));
    assertThat(capturedBulkOperation.getErrorMessage(),equalTo(format(ERROR_MESSAGE_PATTERN, ERROR_UPLOAD_IDENTIFIERS_S3_ISSUE, "error")));
  }

  @Test
  @SneakyThrows
  void shouldThrowExceptionIfOperationIdIsNull() {
    var file = new MockMultipartFile("file", "barcodes.csv", MediaType.TEXT_PLAIN_VALUE, new FileInputStream("src/test/resources/files/modified-user.csv").readAllBytes());
    assertThrows(NotFoundException.class, () -> bulkOperationService.uploadCsvFile(USER, IdentifierType.BARCODE, true, null, UUID.randomUUID(), file));
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

    if (jobStatus == JobStatus.FAILED) {
      when(errorService.uploadErrorsToStorage(bulkOperationId, ERROR_MATCHING_FILE_NAME_PREFIX, "File uploading failed - invalid job status: FAILED (expected: SCHEDULED)"))
        .thenReturn("/linkToMatchingErrorsFile.csv");
    }

    bulkOperationService.uploadCsvFile(USER, IdentifierType.BARCODE, false, null, null, file);
    bulkOperationService.startBulkOperation(bulkOperationId, any(UUID.class), new BulkOperationStart().approach(ApproachType.IN_APP).step(BulkOperationStep.UPLOAD));

    var operationCaptor = ArgumentCaptor.forClass(BulkOperation.class);
    verify(bulkOperationRepository, times(4)).save(operationCaptor.capture());
    assertEquals(OperationStatusType.FAILED, operationCaptor.getAllValues().get(3).getStatus());
    if (jobStatus == JobStatus.FAILED) {
      assertThat(operationCaptor.getValue().getLinkToMatchedRecordsErrorsCsvFile(), equalTo("/linkToMatchingErrorsFile.csv"));
    }
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

    when(errorService.uploadErrorsToStorage(bulkOperationId, ERROR_MATCHING_FILE_NAME_PREFIX,FILE_UPLOADING_FAILED + " : Failed to upload file with identifiers: data export job was not found"))
      .thenReturn("/linkToMatchingErrorsFile.csv");

    bulkOperationService.uploadCsvFile(USER, IdentifierType.BARCODE, false, null, null, file);
    bulkOperationService.startBulkOperation(bulkOperationId, any(UUID.class), new BulkOperationStart().approach(ApproachType.IN_APP).step(BulkOperationStep.UPLOAD));

    var operationCaptor = ArgumentCaptor.forClass(BulkOperation.class);
    Awaitility.await().untilAsserted(() -> verify(bulkOperationRepository, times(4)).save(operationCaptor.capture()));
    assertEquals(OperationStatusType.FAILED, operationCaptor.getAllValues().get(3).getStatus());
    assertThat(operationCaptor.getValue().getLinkToMatchedRecordsErrorsCsvFile(), equalTo("/linkToMatchingErrorsFile.csv"));
  }

  @ParameterizedTest
  @EnumSource(value = ApproachType.class, names = {"IN_APP"}, mode = EnumSource.Mode.INCLUDE)
  @SneakyThrows
  void shouldConfirmChanges(ApproachType approach) {
    try (var context =  new FolioExecutionContextSetter(folioExecutionContext)) {
      var bulkOperationId = UUID.randomUUID();
      var originalPatronGroupId = "3684a786-6671-4268-8ed0-9db82ebca60b";
      var newPatronGroupId = "56c86552-20ec-41d1-964a-5a2be46969e5";
      var pathToTriggering = "/some/path/identifiers.csv";
      var pathToOrigin = "path/origin.json";
      var pathToModified = bulkOperationId + "/json/" + LocalDate.now() + "-Updates-Preview-identifiers.json";
      var pathToOriginalCsv = bulkOperationId + "/origin.csv";
      var pathToModifiedCsv = bulkOperationId + "/" + LocalDate.now() + "-Updates-Preview-CSV-identifiers.csv";
      var pathToUserJson = "src/test/resources/files/user.json";

      when(consortiaService.isTenantCentral(any())).thenReturn(false);

      when(bulkOperationRepository.findById(any(UUID.class)))
        .thenReturn(Optional.of(BulkOperation.builder()
          .id(bulkOperationId)
          .status(DATA_MODIFICATION)
          .entityType(EntityType.USER)
          .identifierType(IdentifierType.BARCODE)
          .linkToTriggeringCsvFile(pathToTriggering)
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
      when(ruleService.getMarcRules(bulkOperationId))
        .thenReturn(new BulkOperationMarcRuleCollection()
          .bulkOperationMarcRules(Collections.emptyList())
          .totalRecords(0));

      when(dataProcessingRepository.save(any(BulkOperationDataProcessing.class)))
        .thenReturn(BulkOperationDataProcessing.builder()
          .bulkOperationId(bulkOperationId)
          .processedNumOfRecords(0)
          .build());

      when(dataProcessingRepository.findAllByBulkOperationId(bulkOperationId))
        .thenReturn(Collections.singletonList(BulkOperationDataProcessing.builder()
          .bulkOperationId(bulkOperationId)
          .status(StatusType.COMPLETED)
          .processedNumOfRecords(1)
          .build()));

      when(remoteFileSystemClient.get(pathToOrigin))
        .thenReturn(new FileInputStream(pathToUserJson));

      when(remoteFileSystemClient.get(pathToModified))
        .thenReturn(new FileInputStream(pathToUserJson));

      // 56c86552-20ec-41d1-964a-5a2be46969e5
      when(groupClient.getGroupById(newPatronGroupId)).thenReturn(new UserGroup().withGroup("original"));
      when(groupClient.getGroupById(originalPatronGroupId)).thenReturn(new UserGroup().withGroup("updated"));

      when(remoteFileSystemClient.writer(pathToModified)).thenReturn(new RemoteStorageWriter(pathToModified, 8192, remoteFolioS3Client));
      when(remoteFileSystemClient.writer(pathToModifiedCsv)).thenReturn(new RemoteStorageWriter(pathToModifiedCsv, 8192, remoteFolioS3Client));

      bulkOperationService.startBulkOperation(bulkOperationId, UUID.randomUUID(), new BulkOperationStart().approach(approach).step(EDIT));

      var expectedPathToModifiedCsvFile = bulkOperationId + "/" + LocalDate.now() + "-Updates-Preview-CSV-identifiers.csv";
      var streamCaptor = ArgumentCaptor.forClass(InputStream.class);
      var pathCaptor = ArgumentCaptor.forClass(String.class);
      Awaitility.await().untilAsserted(() -> verify(remoteFolioS3Client, times(2)).write(pathCaptor.capture(), streamCaptor.capture()));

      assertThat(new String(streamCaptor.getAllValues().get(0).readAllBytes()), containsString(newPatronGroupId));
      assertEquals(expectedPathToModifiedCsvFile, pathCaptor.getAllValues().get(1));

      var dataProcessingCaptor = ArgumentCaptor.forClass(BulkOperationDataProcessing.class);
      Awaitility.await().untilAsserted(() -> verify(dataProcessingRepository, times(2)).save(dataProcessingCaptor.capture()));
      var capturedDataProcessingEntity = dataProcessingCaptor.getAllValues().get(1);
      assertThat(capturedDataProcessingEntity.getProcessedNumOfRecords(), is(1));
      assertThat(capturedDataProcessingEntity.getStatus(), equalTo(StatusType.COMPLETED));
      assertThat(capturedDataProcessingEntity.getEndTime(), notNullValue());

      var bulkOperationCaptor = ArgumentCaptor.forClass(BulkOperation.class);
      Awaitility.await().untilAsserted(() -> verify(bulkOperationRepository, times(5)).save(bulkOperationCaptor.capture()));
      var capturedBulkOperation = bulkOperationCaptor.getValue();
      assertThat(capturedBulkOperation.getLinkToModifiedRecordsCsvFile(), equalTo(expectedPathToModifiedCsvFile));
      assertThat(capturedBulkOperation.getStatus(), equalTo(OperationStatusType.REVIEW_CHANGES));
    }
  }

  @Test
  @SneakyThrows
  void shouldPopulateErrorToBulkOperationIfS3IssuesForConfirmChanges() {
    try (var context =  new FolioExecutionContextSetter(folioExecutionContext)) {
      var bulkOperationId = UUID.randomUUID();
      var newPatronGroupId = "56c86552-20ec-41d1-964a-5a2be46969e5";
      var pathToTriggering = "/some/path/identifiers.csv";
      var pathToOrigin = "path/origin.json";
      var pathToModified = bulkOperationId + "/json/" + LocalDate.now() + "-Updates-Preview-identifiers.json";
      var pathToOriginalCsv = bulkOperationId + "/origin.csv";
      var pathToUserJson = "src/test/resources/files/user.json";

      when(consortiaService.isTenantCentral(any())).thenReturn(false);

      when(bulkOperationRepository.findById(any(UUID.class)))
        .thenReturn(Optional.of(BulkOperation.builder()
          .id(bulkOperationId)
          .status(DATA_MODIFICATION)
          .entityType(EntityType.USER)
          .identifierType(IdentifierType.BARCODE)
          .linkToTriggeringCsvFile(pathToTriggering)
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

      when(ruleService.getMarcRules(bulkOperationId))
        .thenReturn(new BulkOperationMarcRuleCollection()
          .bulkOperationMarcRules(Collections.emptyList())
          .totalRecords(0));

      when(dataProcessingRepository.save(any(BulkOperationDataProcessing.class)))
        .thenReturn(BulkOperationDataProcessing.builder()
          .bulkOperationId(bulkOperationId)
          .processedNumOfRecords(0)
          .build());

      when(remoteFileSystemClient.get(pathToOrigin))
        .thenThrow(new S3ClientException("error"));

      when(remoteFileSystemClient.get(pathToModified))
        .thenReturn(new FileInputStream(pathToUserJson));

      when(errorService.uploadErrorsToStorage(bulkOperationId, ERROR_COMMITTING_FILE_NAME_PREFIX,ERROR_NOT_CONFIRM_CHANGES_S3_ISSUE + " : error"))
        .thenReturn("/linkToCommittingErrorsFile.csv");

      bulkOperationService.startBulkOperation(bulkOperationId, UUID.randomUUID(), new BulkOperationStart().approach(ApproachType.IN_APP).step(EDIT));

      var bulkOperationCaptor = ArgumentCaptor.forClass(BulkOperation.class);
      Awaitility.await().untilAsserted(() -> verify(bulkOperationRepository, times(5)).save(bulkOperationCaptor.capture()));
      var capturedBulkOperation = bulkOperationCaptor.getValue();

      assertThat(capturedBulkOperation.getStatus(), equalTo(OperationStatusType.FAILED));
      assertThat(capturedBulkOperation.getErrorMessage(),equalTo(format(ERROR_MESSAGE_PATTERN, ERROR_NOT_CONFIRM_CHANGES_S3_ISSUE, "error")));
      assertThat(capturedBulkOperation.getLinkToCommittedRecordsErrorsCsvFile(), equalTo("/linkToCommittingErrorsFile.csv"));
    }
  }

  @Test
  @SneakyThrows
  void shouldConfirmChangesForInstanceMarc() {
    try (var context =  new FolioExecutionContextSetter(folioExecutionContext)) {
      var bulkOperationId = UUID.randomUUID();
      var marcAction = new MarcAction();
      marcAction.setName(UpdateActionType.CLEAR_FIELD);
      var bulkOperationMarcRule = new BulkOperationMarcRule();
      bulkOperationMarcRule.setBulkOperationId(bulkOperationId);
      bulkOperationMarcRule.setActions(List.of(marcAction));
      bulkOperationMarcRule.setTag("500");
      bulkOperationMarcRule.setInd1("ind1");
      bulkOperationMarcRule.setInd2("ind2");

      var bulkOperationMarcRuleCollection = new BulkOperationMarcRuleCollection();
      bulkOperationMarcRuleCollection.setBulkOperationMarcRules(List.of(bulkOperationMarcRule));

      var pathToTriggering = "/some/path/instance_marc.csv";
      var pathToMatchedRecordsMarcFile = "/some/path/Marc-Records-instance_marc.mrc";
      var pathToMatchedJson = bulkOperationId + "/instance_marc.json";
      var pathToModifiedRecordsMarcFileName= "Updates-Preview-Marc-Records-instance_marc.mrc";
      var pathToInstanceMarc = "src/test/resources/files/instance_marc.mrc";
      var pathToInstanceJson = "src/test/resources/files/instance_marc.json";
      var pathToFilteredInstanceJson = "src/test/resources/files/instance_marc_filtered.json";
      var expectedPathToModifiedMarcFile = bulkOperationId + "/" + LocalDate.now() + "-Updates-Preview-MARC-instance_marc.mrc";
      var expectedPathToTmpModifiedJson = TMP_MATCHED_JSON_PATH_TEMPLATE.formatted(bulkOperationId);

      when(bulkOperationRepository.findById(any(UUID.class)))
        .thenReturn(Optional.of(BulkOperation.builder()
          .id(bulkOperationId)
          .status(DATA_MODIFICATION)
          .entityType(EntityType.INSTANCE_MARC)
          .identifierType(IdentifierType.ID)
          .linkToTriggeringCsvFile(pathToTriggering)
          .linkToMatchedRecordsJsonFile(pathToMatchedJson)
          .linkToMatchedRecordsMarcFile(pathToMatchedRecordsMarcFile)
          .linkToModifiedRecordsMarcFile(pathToModifiedRecordsMarcFileName)
          .processedNumOfRecords(0)
          .build()));
      when(ruleService.getMarcRules(bulkOperationId))
        .thenReturn(bulkOperationMarcRuleCollection);
      when(ruleService.getRules(bulkOperationId))
        .thenReturn(new BulkOperationRuleCollection()
          .bulkOperationRules(Collections.emptyList())
          .totalRecords(0));
      when(dataProcessingRepository.save(any(BulkOperationDataProcessing.class)))
        .thenReturn(BulkOperationDataProcessing.builder()
          .bulkOperationId(bulkOperationId)
          .processedNumOfRecords(0)
          .build());
      when(dataProcessingRepository.findAllByBulkOperationId(bulkOperationId))
        .thenReturn(Collections.singletonList(BulkOperationDataProcessing.builder()
          .bulkOperationId(bulkOperationId)
          .status(StatusType.COMPLETED)
          .processedNumOfRecords(1)
          .build()));
      when(remoteFileSystemClient.get(pathToMatchedRecordsMarcFile))
        .thenReturn(new FileInputStream(pathToInstanceMarc));
      when(remoteFileSystemClient.get(pathToMatchedJson))
        .thenReturn(new FileInputStream(pathToInstanceJson))
        .thenReturn(new FileInputStream(pathToFilteredInstanceJson));
      when(remoteFileSystemClient.get(expectedPathToTmpModifiedJson))
        .thenReturn(new FileInputStream(pathToInstanceJson));
      when(remoteFileSystemClient.writer(anyString()))
        .thenReturn(new StringWriter());
      when(remoteFileSystemClient.marcWriter(expectedPathToModifiedMarcFile)).thenReturn(new MarcRemoteStorageWriter(new RemoteStorageWriter(expectedPathToModifiedMarcFile, 8192, remoteFolioS3Client)));
      when(marcCsvHelper.getModifiedDataForCsv(any(Record.class)))
        .thenReturn(new String[]{"a", "b", "c"});

      bulkOperationService.startBulkOperation(bulkOperationId, UUID.randomUUID(), new BulkOperationStart().approach(ApproachType.IN_APP).step(EDIT));


      var streamCaptor = ArgumentCaptor.forClass(InputStream.class);
      var pathCaptor = ArgumentCaptor.forClass(String.class);
      Awaitility.await().untilAsserted(() -> verify(remoteFolioS3Client, times(1)).write(pathCaptor.capture(), streamCaptor.capture()));

      var dataProcessingCaptor = ArgumentCaptor.forClass(BulkOperationDataProcessing.class);
      Awaitility.await().untilAsserted(() -> verify(dataProcessingRepository, times(4)).save(dataProcessingCaptor.capture()));
      var capturedDataProcessingEntity = dataProcessingCaptor.getAllValues().get(2);
      assertThat(capturedDataProcessingEntity.getProcessedNumOfRecords(), is(1));
      assertThat(capturedDataProcessingEntity.getStatus(), equalTo(StatusType.COMPLETED));
      assertThat(capturedDataProcessingEntity.getEndTime(), notNullValue());

      var bulkOperationCaptor = ArgumentCaptor.forClass(BulkOperation.class);
      Awaitility.await().untilAsserted(() -> verify(bulkOperationRepository, times(6)).save(bulkOperationCaptor.capture()));
      var capturedBulkOperation = bulkOperationCaptor.getValue();
      assertThat(capturedBulkOperation.getLinkToModifiedRecordsMarcFile(), equalTo(expectedPathToModifiedMarcFile));
      assertThat(capturedBulkOperation.getStatus(), equalTo(OperationStatusType.REVIEW_CHANGES));

      var identifierArgumentCaptor = ArgumentCaptor.forClass(String.class);
      var errorMessageArgumentCaptor = ArgumentCaptor.forClass(String.class);
      Awaitility.await().untilAsserted(() -> verify(errorService).saveError(eq(bulkOperationId), identifierArgumentCaptor.capture(),
        errorMessageArgumentCaptor.capture(), eq(ErrorType.ERROR)));
      Assertions.assertThat(errorMessageArgumentCaptor.getValue()).isEqualTo(MSG_BULK_EDIT_SUPPORTED_FOR_MARC_ONLY.formatted("FOLIO"));
      Assertions.assertThat(identifierArgumentCaptor.getValue()).isEqualTo("69640328-788e-43fc-9c3c-af39e243f3b7");
    }
  }

  @Test
  @SneakyThrows
  void shouldNotBeProcessedRecordsIfUploadedNotMarcAndSelectedMarcEdit() {
    try (var context =  new FolioExecutionContextSetter(folioExecutionContext)) {
      var bulkOperationId = UUID.randomUUID();
      var marcAction = new MarcAction();
      marcAction.setName(UpdateActionType.CLEAR_FIELD);
      var bulkOperationMarcRule = new BulkOperationMarcRule();
      bulkOperationMarcRule.setBulkOperationId(bulkOperationId);
      bulkOperationMarcRule.setActions(List.of(marcAction));
      bulkOperationMarcRule.setTag("500");
      bulkOperationMarcRule.setInd1("ind1");
      bulkOperationMarcRule.setInd2("ind2");

      var bulkOperationMarcRuleCollection = new BulkOperationMarcRuleCollection();
      bulkOperationMarcRuleCollection.setBulkOperationMarcRules(List.of(bulkOperationMarcRule));

      var pathToTriggering = "/some/path/instance_marc.csv";
      String pathToMatchedRecordsMarcFile = null;
      var pathToMatchedJson = bulkOperationId + "/instance_marc.json";
      var pathToModifiedRecordsMarcFileName= "Updates-Preview-Marc-Records-instance_marc.mrc";
      var pathToInstanceMarc = "src/test/resources/files/instance_marc.mrc";
      var pathToInstanceJson = "src/test/resources/files/instance_marc.json";
      var expectedPathToModifiedMarcFile = bulkOperationId + "/" + LocalDate.now() + "-Updates-Preview-MARC-instance_marc.mrc";

      when(bulkOperationRepository.findById(any(UUID.class)))
        .thenReturn(Optional.of(BulkOperation.builder()
          .id(bulkOperationId)
          .status(DATA_MODIFICATION)
          .entityType(EntityType.INSTANCE_MARC)
          .identifierType(IdentifierType.ID)
          .linkToTriggeringCsvFile(pathToTriggering)
          .linkToMatchedRecordsJsonFile(pathToMatchedJson)
          .linkToMatchedRecordsMarcFile(pathToMatchedRecordsMarcFile)
          .linkToModifiedRecordsMarcFile(pathToModifiedRecordsMarcFileName)
          .processedNumOfRecords(0)
          .build()));
      when(ruleService.getMarcRules(bulkOperationId))
        .thenReturn(bulkOperationMarcRuleCollection);
      when(ruleService.getRules(bulkOperationId))
        .thenReturn(new BulkOperationRuleCollection()
          .bulkOperationRules(Collections.emptyList())
          .totalRecords(0));
      when(dataProcessingRepository.save(any(BulkOperationDataProcessing.class)))
        .thenReturn(BulkOperationDataProcessing.builder()
          .bulkOperationId(bulkOperationId)
          .processedNumOfRecords(0)
          .build());
      when(dataProcessingRepository.findAllByBulkOperationId(bulkOperationId))
        .thenReturn(Collections.singletonList(BulkOperationDataProcessing.builder()
          .bulkOperationId(bulkOperationId)
          .status(StatusType.COMPLETED)
          .processedNumOfRecords(1)
          .build()));
      when(remoteFileSystemClient.get(pathToMatchedRecordsMarcFile))
        .thenReturn(new FileInputStream(pathToInstanceMarc));
      when(remoteFileSystemClient.get(pathToMatchedJson))
        .thenReturn(new FileInputStream(pathToInstanceJson));
      when(remoteFileSystemClient.writer(anyString()))
        .thenReturn(new StringWriter());
      when(remoteFileSystemClient.marcWriter(expectedPathToModifiedMarcFile)).thenReturn(new MarcRemoteStorageWriter(new RemoteStorageWriter(expectedPathToModifiedMarcFile, 8192, remoteFolioS3Client)));

      bulkOperationService.startBulkOperation(bulkOperationId, UUID.randomUUID(), new BulkOperationStart().approach(ApproachType.IN_APP).step(EDIT));

      var dataProcessingCaptor = ArgumentCaptor.forClass(BulkOperationDataProcessing.class);
      Awaitility.await().untilAsserted(() -> verify(dataProcessingRepository, times(4)).save(dataProcessingCaptor.capture()));
      var capturedDataProcessingEntity = dataProcessingCaptor.getAllValues().get(2);
      assertThat(capturedDataProcessingEntity.getProcessedNumOfRecords(), is(0));
    }
  }

  @Test
  @SneakyThrows
  void  shouldPopulateErrorToBulkOperationIfS3IssuesForConfirmChangesForInstanceMarc() {
    try (var context =  new FolioExecutionContextSetter(folioExecutionContext)) {
      var bulkOperationId = UUID.randomUUID();
      var marcAction = new MarcAction();
      marcAction.setName(UpdateActionType.CLEAR_FIELD);
      var bulkOperationMarcRule = new BulkOperationMarcRule();
      bulkOperationMarcRule.setBulkOperationId(bulkOperationId);
      bulkOperationMarcRule.setActions(List.of(marcAction));
      bulkOperationMarcRule.setTag("tag");
      bulkOperationMarcRule.setInd1("ind1");
      bulkOperationMarcRule.setInd2("ind2");

      var bulkOperationMarcRuleCollection = new BulkOperationMarcRuleCollection();
      bulkOperationMarcRuleCollection.setBulkOperationMarcRules(List.of(bulkOperationMarcRule));

      var pathToTriggering = "/some/path/instance_marc.csv";
      var pathToMatchedRecordsMarcFile = "/some/path/Marc-Records-instance_marc.mrc";
      var pathToMatchedRecordsJsonFile = "/some/path/Marc-Records-instance_marc.json";
      var pathToInstanceJson = "src/test/resources/files/instance_marc.json";
      var pathToOriginalCsv = bulkOperationId + "/origin.csv";
      var pathToModifiedRecordsMarcFileName= "Updates-Preview-Marc-Records-instance_marc.mrc";
      var expectedPathToModifiedMarcFile = bulkOperationId + "/" + LocalDate.now() + "-Updates-Preview-Marc-Records-instance_marc.mrc";

      when(bulkOperationRepository.findById(any(UUID.class)))
        .thenReturn(Optional.of(BulkOperation.builder()
          .id(bulkOperationId)
          .status(DATA_MODIFICATION)
          .entityType(EntityType.INSTANCE_MARC)
          .identifierType(IdentifierType.ID)
          .linkToTriggeringCsvFile(pathToTriggering)
          .linkToMatchedRecordsCsvFile(pathToOriginalCsv)
          .linkToMatchedRecordsJsonFile(pathToMatchedRecordsJsonFile)
          .linkToMatchedRecordsMarcFile(pathToMatchedRecordsMarcFile)
          .linkToModifiedRecordsMarcFile(pathToModifiedRecordsMarcFileName)
          .processedNumOfRecords(0)
          .build()));
      when(ruleService.getMarcRules(bulkOperationId))
        .thenReturn(bulkOperationMarcRuleCollection);
      when(ruleService.getRules(bulkOperationId))
        .thenReturn(new BulkOperationRuleCollection()
          .bulkOperationRules(Collections.emptyList())
          .totalRecords(0));
      when(dataProcessingRepository.save(any(BulkOperationDataProcessing.class)))
        .thenReturn(BulkOperationDataProcessing.builder()
          .bulkOperationId(bulkOperationId)
          .processedNumOfRecords(0)
          .build());
      when(remoteFileSystemClient.get(pathToMatchedRecordsJsonFile))
        .thenReturn(new FileInputStream(pathToInstanceJson));
      when(remoteFileSystemClient.writer(anyString()))
        .thenReturn(new StringWriter());
      when(remoteFileSystemClient.get(pathToMatchedRecordsMarcFile))
        .thenThrow(new S3ClientException("error"));
      when(remoteFileSystemClient.marcWriter(expectedPathToModifiedMarcFile)).thenReturn(new MarcRemoteStorageWriter(new RemoteStorageWriter(expectedPathToModifiedMarcFile, 8192, remoteFolioS3Client)));

      bulkOperationService.startBulkOperation(bulkOperationId, UUID.randomUUID(), new BulkOperationStart().approach(ApproachType.IN_APP).step(EDIT));

      var bulkOperationCaptor = ArgumentCaptor.forClass(BulkOperation.class);
      Awaitility.await().untilAsserted(() -> verify(bulkOperationRepository, atLeast(6)).save(bulkOperationCaptor.capture()));
      var capturedBulkOperation = bulkOperationCaptor.getValue();
      assertThat(capturedBulkOperation.getStatus(), equalTo(OperationStatusType.FAILED));
      assertThat(capturedBulkOperation.getErrorMessage(),equalTo(format(ERROR_MESSAGE_PATTERN, ERROR_NOT_CONFIRM_CHANGES_S3_ISSUE, "error")));
    }
  }

  @Test
  @SneakyThrows
  void shouldFailConfirmChangesForInstanceMarcIfMarcWriterNotAvailable() {
    try (var context =  new FolioExecutionContextSetter(folioExecutionContext)) {
      var bulkOperationId = UUID.randomUUID();
      var marcAction = new MarcAction();
      marcAction.setName(UpdateActionType.CLEAR_FIELD);
      var bulkOperationMarcRule = new BulkOperationMarcRule();
      bulkOperationMarcRule.setBulkOperationId(bulkOperationId);
      bulkOperationMarcRule.setActions(List.of(marcAction));
      bulkOperationMarcRule.setTag("tag");
      bulkOperationMarcRule.setInd1("ind1");
      bulkOperationMarcRule.setInd2("ind2");

      var bulkOperationMarcRuleCollection = new BulkOperationMarcRuleCollection();
      bulkOperationMarcRuleCollection.setBulkOperationMarcRules(List.of(bulkOperationMarcRule));

      var pathToTriggering = "/some/path/instance_marc.csv";
      var pathToMatchedRecordsMarcFile = "/some/path/Marc-Records-instance_marc.mrc";
      var pathToMatchedRecordsJsonFile = "/some/path/Marc-Records-instance_marc.json";
      var pathToOriginalJson = bulkOperationId + "/instance_marc.json";
      var pathToModifiedRecordsMarcFileName= "Updates-Preview-Marc-Records-instance_marc.mrc";
      var pathToInstanceMarc = "src/test/resources/files/instance_marc.mrc";
      var pathToInstanceJson = "src/test/resources/files/instance_marc.json";

      when(bulkOperationRepository.findById(any(UUID.class)))
        .thenReturn(Optional.of(BulkOperation.builder()
          .id(bulkOperationId)
          .status(DATA_MODIFICATION)
          .entityType(EntityType.INSTANCE_MARC)
          .identifierType(IdentifierType.ID)
          .linkToTriggeringCsvFile(pathToTriggering)
          .linkToMatchedRecordsCsvFile(pathToOriginalJson)
          .linkToMatchedRecordsMarcFile(pathToMatchedRecordsMarcFile)
          .linkToMatchedRecordsJsonFile(pathToMatchedRecordsJsonFile)
          .linkToModifiedRecordsMarcFile(pathToModifiedRecordsMarcFileName)
          .processedNumOfRecords(0)
          .build()));
      when(ruleService.getMarcRules(bulkOperationId))
        .thenReturn(bulkOperationMarcRuleCollection);
      when(ruleService.getRules(bulkOperationId))
        .thenReturn(new BulkOperationRuleCollection().bulkOperationRules(Collections.emptyList()).totalRecords(0));
      when(dataProcessingRepository.save(any(BulkOperationDataProcessing.class)))
        .thenReturn(BulkOperationDataProcessing.builder()
          .bulkOperationId(bulkOperationId)
          .processedNumOfRecords(0)
          .build());
      when(remoteFileSystemClient.get(pathToMatchedRecordsJsonFile))
        .thenReturn(new FileInputStream(pathToInstanceJson));
      when(remoteFileSystemClient.writer(anyString()))
        .thenReturn(new StringWriter());
      when(remoteFileSystemClient.get(pathToMatchedRecordsMarcFile))
        .thenReturn(new FileInputStream(pathToInstanceMarc));
      when(remoteFileSystemClient.marcWriter(anyString())).thenThrow(new RuntimeException("error"));
      when(remoteFileSystemClient.writer(anyString())).thenReturn(new StringWriter());
      when(errorService.uploadErrorsToStorage(bulkOperationId, ERROR_COMMITTING_FILE_NAME_PREFIX, "Confirm failed : error"))
        .thenReturn("/linkToCommittingErrorsFile.csv");

      bulkOperationService.startBulkOperation(bulkOperationId, UUID.randomUUID(), new BulkOperationStart().approach(ApproachType.IN_APP).step(EDIT));

      var bulkOperationCaptor = ArgumentCaptor.forClass(BulkOperation.class);
      Awaitility.await().untilAsserted(() -> verify(bulkOperationRepository, times(7)).save(bulkOperationCaptor.capture()));
      var capturedBulkOperation = bulkOperationCaptor.getValue();
      assertThat(capturedBulkOperation.getStatus(), equalTo(OperationStatusType.FAILED));
      assertThat(capturedBulkOperation.getLinkToCommittedRecordsErrorsCsvFile(), equalTo("/linkToCommittingErrorsFile.csv"));
    }
  }

  @ParameterizedTest
  @EnumSource(value = ApproachType.class, names = {"IN_APP"}, mode = EnumSource.Mode.INCLUDE)
  void shouldConfirmChangesForItemWhenValidationErrorAndOtherValidChangesExist(ApproachType approach) throws FileNotFoundException {
    try (var context =  new FolioExecutionContextSetter(folioExecutionContext)) {
      var bulkOperationId = UUID.randomUUID();
      var pathToTriggering = "/some/path/identifiers.csv";
      var pathToOrigin = "path/origin.json";
      var pathToModified = bulkOperationId + "/json/" + LocalDate.now() + "-Updates-Preview-identifiers.json";
      var pathToOriginalCsv = bulkOperationId + "/origin.csv";
      var pathToModifiedCsv = bulkOperationId + "/" + LocalDate.now() + "-Updates-Preview-CSV-identifiers.csv";
      var pathToItemJson = "src/test/resources/files/item.json";

      mockHoldingsClient();
      mockLocationClient();

      when(consortiaService.isTenantCentral(any())).thenReturn(false);
      when(bulkOperationRepository.findById(bulkOperationId))
        .thenReturn(Optional.of(BulkOperation.builder()
          .id(bulkOperationId)
          .status(DATA_MODIFICATION)
          .entityType(EntityType.ITEM)
          .identifierType(IdentifierType.BARCODE)
          .linkToTriggeringCsvFile(pathToTriggering)
          .linkToMatchedRecordsJsonFile(pathToOrigin)
          .linkToModifiedRecordsJsonFile("existing.csv")
          .linkToModifiedRecordsCsvFile("existing.json")
          .linkToMatchedRecordsCsvFile(pathToOriginalCsv)
          .processedNumOfRecords(0)
          .committedNumOfErrors(0)
          .build()));

      var tempLocationRules = new BulkOperationRule()
        .bulkOperationId(bulkOperationId)
        .ruleDetails(new BulkOperationRuleRuleDetails().
          option(UpdateOptionType.TEMPORARY_LOCATION)
          .actions(List.of(new Action()
            .type(UpdateActionType.REPLACE_WITH)
            .updated("fcd51ce2-1111-48f0-840e-89ffa2288371"))));
      var statusRules = new BulkOperationRule()
        .bulkOperationId(bulkOperationId)
        .ruleDetails(new BulkOperationRuleRuleDetails()
          .option(UpdateOptionType.STATUS)
          .actions(List.of(new Action()
            .type(UpdateActionType.REPLACE_WITH)
            .updated("Available"))));

      when(itemReferenceService.getLocationById(any(), any())).thenReturn(new ItemLocation()
        .withId("fcd51ce2-1111-48f0-840e-89ffa2288371").withName("Annex"));

      when(ruleService.getRules(bulkOperationId))
        .thenReturn(new BulkOperationRuleCollection()
          .bulkOperationRules(List.of(tempLocationRules, statusRules))
          .totalRecords(2));
      when(ruleService.getMarcRules(bulkOperationId))
        .thenReturn(new BulkOperationMarcRuleCollection()
          .bulkOperationMarcRules(Collections.emptyList())
          .totalRecords(0));

      when(dataProcessingRepository.save(any(BulkOperationDataProcessing.class)))
        .thenReturn(BulkOperationDataProcessing.builder()
          .bulkOperationId(bulkOperationId)
          .processedNumOfRecords(0)
          .build());

      when(dataProcessingRepository.findAllByBulkOperationId(bulkOperationId))
        .thenReturn(Collections.singletonList(BulkOperationDataProcessing.builder()
          .bulkOperationId(bulkOperationId)
          .status(StatusType.COMPLETED)
          .processedNumOfRecords(1)
          .build()));

      when(remoteFileSystemClient.get(pathToOrigin))
        .thenReturn(new FileInputStream(pathToItemJson));

      when(remoteFileSystemClient.get(pathToModified))
        .thenReturn(new FileInputStream(pathToItemJson));

      when(remoteFileSystemClient.writer(pathToModified)).thenReturn(new RemoteStorageWriter(pathToModified, 8192, remoteFolioS3Client));
      when(remoteFileSystemClient.writer(pathToModifiedCsv)).thenReturn(new RemoteStorageWriter(pathToModifiedCsv, 8192, remoteFolioS3Client));

      bulkOperationService.startBulkOperation(bulkOperationId, UUID.randomUUID(), new BulkOperationStart().approach(approach).step(EDIT));

      var expectedPathToModifiedCsvFile = bulkOperationId + "/" + LocalDate.now() + "-Updates-Preview-CSV-identifiers.csv";
      var streamCaptor = ArgumentCaptor.forClass(InputStream.class);
      var pathCaptor = ArgumentCaptor.forClass(String.class);

      Awaitility.await().untilAsserted(() -> verify(remoteFolioS3Client, times(2)).write(pathCaptor.capture(), streamCaptor.capture()));
      assertEquals(expectedPathToModifiedCsvFile, pathCaptor.getAllValues().get(1));

      var dataProcessingCaptor = ArgumentCaptor.forClass(BulkOperationDataProcessing.class);
      Awaitility.await().untilAsserted(() -> verify(dataProcessingRepository, times(2)).save(dataProcessingCaptor.capture()));
      var capturedDataProcessingEntity = dataProcessingCaptor.getAllValues().get(1);
      assertThat(capturedDataProcessingEntity.getProcessedNumOfRecords(), is(1));

      assertThat(capturedDataProcessingEntity.getStatus(), equalTo(StatusType.COMPLETED));
      assertThat(capturedDataProcessingEntity.getEndTime(), notNullValue());

      verify(errorService).saveError(eq(bulkOperationId), eq("10101"), any(String.class), eq(ErrorType.ERROR));

      var bulkOperationCaptor = ArgumentCaptor.forClass(BulkOperation.class);
      Awaitility.await().untilAsserted(() -> verify(bulkOperationRepository, times(5)).save(bulkOperationCaptor.capture()));
      var capturedBulkOperation = bulkOperationCaptor.getValue();
      assertThat(capturedBulkOperation.getLinkToModifiedRecordsCsvFile(), equalTo(expectedPathToModifiedCsvFile));
      assertThat(capturedBulkOperation.getProcessedNumOfRecords(), equalTo(1));
      assertThat(capturedBulkOperation.getStatus(), equalTo(OperationStatusType.REVIEW_CHANGES));
    }
  }

  private void mockLocationClient() {
    when(locationClient.getLocationById(any())).thenReturn(
      ItemLocation.builder()
        .name("Main Library")
        .build()
    );
  }

  private void mockHoldingsClient() {
    when(holdingsClient.getHoldingById(any())).thenReturn(
      HoldingsRecord.builder()
        .effectiveLocationId(UUID.randomUUID().toString())
        .callNumber("TK5105.88815 . A58 2004 FT MEADE")
        .build()
    );
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

    var rules = new BulkOperationRuleCollection()
        .bulkOperationRules(List.of(new BulkOperationRule()
          .ruleDetails(new BulkOperationRuleRuleDetails()
            .option(UpdateOptionType.PATRON_GROUP)
            .actions(List.of(new Action()
              .type(UpdateActionType.REPLACE_WITH)
              .updated(newPatronGroupId))))))
        .totalRecords(1);
    when(ruleService.getRules(bulkOperationId)).thenReturn(rules);

    var dataProcessing = BulkOperationDataProcessing.builder()
      .bulkOperationId(bulkOperationId)
      .processedNumOfRecords(0)
      .build();

    when(dataProcessingRepository.save(any(BulkOperationDataProcessing.class)))
      .thenReturn(dataProcessing);

    when(remoteFileSystemClient.get(pathToOrigin))
      .thenThrow(new RuntimeException("Failed to read file"));

    bulkOperationService.confirm(dataProcessing);

    var bulkOperationCaptor = ArgumentCaptor.forClass(BulkOperation.class);
    verify(bulkOperationRepository, times(2)).save(bulkOperationCaptor.capture());
    var capturedBulkOperation = bulkOperationCaptor.getValue();
    assertThat(capturedBulkOperation.getStatus(), equalTo(OperationStatusType.FAILED));
    assertThat(capturedBulkOperation.getEndTime(), notNullValue());
    assertThat(capturedBulkOperation.getErrorMessage(), notNullValue());
  }

  @ParameterizedTest
  @CsvSource(value = {"'',COMPLETED", "path/to/file,COMPLETED_WITH_ERRORS"}, delimiter = ',')
  @SneakyThrows
  void shouldCommitChanges(String linkToErrors, OperationStatusType statusType) {
    try (var context =  new FolioExecutionContextSetter(folioExecutionContext)) {

      var bulkOperationId = UUID.randomUUID();
      var pathToTriggering = "/some/path/identifiers.csv";
      var pathToOrigin = bulkOperationId + "/json/origin.json";
      var pathToModified = bulkOperationId + "/json/modified-origin.json";
      var pathToModifiedResult = bulkOperationId + "/json/" + LocalDate.now() + "-Changed-Records-identifiers.json";
      var pathToModifiedCsv = bulkOperationId + "/modified-origin.csv";
      var pathToModifiedCsvResult = bulkOperationId + "/" + LocalDate.now() + "-Changed-Records-identifiers.csv";
      var pathToUserJson = "src/test/resources/files/user.json";
      var pathToModifiedUserJson = "src/test/resources/files/modified-user.json";

      doNothing().when(permissionsValidator).checkIfBulkEditWritePermissionExists(anyString(), any(), anyString());

      when(bulkOperationRepository.findById(any(UUID.class)))
        .thenReturn(Optional.of(BulkOperation.builder()
          .id(bulkOperationId)
          .entityType(USER)
          .status(REVIEW_CHANGES)
          .identifierType(IdentifierType.BARCODE)
          .linkToTriggeringCsvFile(pathToTriggering)
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
          .linkToTriggeringCsvFile(pathToTriggering)
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

      var expectedPathToResultFile = bulkOperationId + "/json/" + LocalDate.now() + "-Changed-Records-identifiers.json";
      when(remoteFileSystemClient.get(expectedPathToResultFile))
        .thenReturn(new FileInputStream(pathToModifiedUserJson));

      when(executionContentRepository.save(any(BulkOperationExecutionContent.class)))
        .thenReturn(BulkOperationExecutionContent.builder().build());

      when(groupClient.getGroupById("cdd8a5c8-dce7-4d7f-859a-83754b36c740")).thenReturn(new UserGroup());

      when(remoteFileSystemClient.writer(pathToModifiedResult)).thenReturn(new RemoteStorageWriter(pathToModifiedResult, 8192, remoteFolioS3Client));
      when(remoteFileSystemClient.writer(pathToModifiedCsvResult)).thenReturn(new RemoteStorageWriter(pathToModifiedCsvResult, 8192, remoteFolioS3Client));

      when(errorService.uploadErrorsToStorage(any(UUID.class), any(String.class), any())).thenReturn(linkToErrors);

      bulkOperationService.startBulkOperation(bulkOperationId, UUID.randomUUID(), new BulkOperationStart().approach(ApproachType.IN_APP).step(COMMIT));

      Awaitility.await().untilAsserted(() -> verify(userClient).updateUser(any(User.class), anyString()));

      var streamCaptor = ArgumentCaptor.forClass(InputStream.class);
      var pathCaptor = ArgumentCaptor.forClass(String.class);
      Awaitility.await().untilAsserted(() -> verify(remoteFolioS3Client, times(1)).write(pathCaptor.capture(), streamCaptor.capture()));
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
      assertThat(secondCapture.getStatus(), equalTo(statusType));
      assertThat(secondCapture.getEndTime(), notNullValue());
    }
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
    try (var context =  new FolioExecutionContextSetter(folioExecutionContext)) {
      var bulkOperationId = UUID.randomUUID();
      var pathToTriggering = "/some/path/identifiers.csv";
      var pathToOrigin = bulkOperationId + "/json/origin.json";
      var pathToModifiedCsv = bulkOperationId + "/modified-origin.csv";
      var expectedPathToResultFile = bulkOperationId + "/json/" + LocalDate.now() + "-Updates-Preview-identifiers.json";
      var pathToUserJson = "src/test/resources/files/user.json";
      var pathToModifiedUserCsv = "src/test/resources/files/modified-user.csv";
      var pathToTempFile = bulkOperationId + "/temp.txt";

      when(bulkOperationRepository.findById(any(UUID.class)))
        .thenReturn(Optional.of(BulkOperation.builder()
          .id(bulkOperationId)
          .entityType(USER)
          .status(DATA_MODIFICATION)
          .identifierType(IdentifierType.BARCODE)
          .linkToTriggeringCsvFile(pathToTriggering)
          .linkToMatchedRecordsJsonFile(pathToOrigin)
          .linkToModifiedRecordsCsvFile(pathToModifiedCsv)
          .processedNumOfRecords(0)
          .build()));

      when(remoteFileSystemClient.get(pathToOrigin))
        .thenReturn(new FileInputStream(pathToUserJson));

      when(remoteFileSystemClient.get(pathToModifiedCsv))
        .thenReturn(new FileInputStream(pathToModifiedUserCsv));

      when(groupClient.getByQuery(format("group==\"%s\"", "staff"))).thenReturn(new UserGroupCollection().withUsergroups(List.of(new UserGroup())));

      when(remoteFileSystemClient.writer(anyString()))
        .thenReturn(new RemoteStorageWriter(pathToTempFile, 8192, remoteFolioS3Client));

      bulkOperationService.startBulkOperation(bulkOperationId, UUID.randomUUID(), new BulkOperationStart().approach(ApproachType.MANUAL).step(EDIT));

      var operationCaptor = ArgumentCaptor.forClass(BulkOperation.class);
      Awaitility.await().untilAsserted(() -> verify(bulkOperationRepository, times(2)).save(operationCaptor.capture()));
      var capture = operationCaptor.getAllValues().get(0);
      assertThat(capture.getStatus(), equalTo(OperationStatusType.REVIEW_CHANGES));
      assertThat(capture.getLinkToModifiedRecordsJsonFile(), equalTo(expectedPathToResultFile));
    }
  }

  @Test
  @SneakyThrows
  void shouldNotUpdateIfEntitiesAreEqual() {
    var bulkOperationId = UUID.randomUUID();
    var pathToTriggering = "/some/path/identifiers.csv";
    var pathToOrigin = bulkOperationId + "/origin.json";
    var pathToOriginCsv = bulkOperationId + "/origin.csv";
    var pathToModified = bulkOperationId + "/modified-origin.json";
    var pathToUserJson = "src/test/resources/files/user2.json";

    var operation = BulkOperation.builder()
      .id(bulkOperationId)
      .entityType(USER)
      .identifierType(IdentifierType.BARCODE)
      .linkToTriggeringCsvFile(pathToTriggering)
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

    var expectedPathToResultFile = bulkOperationId + "/json/" + LocalDate.now() + "-Changed-Records-identifiers.json";
    var expectedPathToResultCsvFile = bulkOperationId + "/" + LocalDate.now() + "-Changed-Records-CSV-identifiers.csv";

    var jsonWriter = new StringWriter();
    when(remoteFileSystemClient.writer(expectedPathToResultFile))
      .thenReturn(jsonWriter);
    var csvWriter = new StringWriter();
    when(remoteFileSystemClient.writer(expectedPathToResultCsvFile))
      .thenReturn(csvWriter);

    when(executionContentRepository.save(any(BulkOperationExecutionContent.class)))
      .thenReturn(BulkOperationExecutionContent.builder().build());

    bulkOperationService.commit(operation);

    verify(userClient, times(0)).updateUser(any(User.class), anyString());

    Awaitility.await().untilAsserted(() -> verify(errorService, times(1)).saveError(eq(bulkOperationId), anyString(), eq(MSG_NO_CHANGE_REQUIRED), eq(ErrorType.WARNING)));

    var pathCaptor = ArgumentCaptor.forClass(String.class);
    Awaitility.await().untilAsserted(() -> verify(remoteFileSystemClient, times(2)).writer(pathCaptor.capture()));
    assertEquals(expectedPathToResultCsvFile, pathCaptor.getAllValues().get(0));
    assertEquals(expectedPathToResultFile, pathCaptor.getAllValues().get(1));
    assertNull(operation.getLinkToCommittedRecordsCsvFile());
    assertEquals(StringUtils.EMPTY, jsonWriter.toString());
    assertEquals(StringUtils.EMPTY, csvWriter.toString());
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
    var pathToTempFile = bulkOperationId + "/temp.txt";

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

    when(remoteFileSystemClient.writer(anyString())).thenReturn(new RemoteStorageWriter(pathToTempFile, 8192, remoteFolioS3Client));

    when(executionContentRepository.save(any(BulkOperationExecutionContent.class)))
      .thenReturn(BulkOperationExecutionContent.builder().build());

    doThrow(new BadRequestException("Bad request")).when(userClient).updateUser(any(User.class), anyString());

    bulkOperationService.commit(operation);

    Awaitility.await().untilAsserted(() -> verify(errorService, times(1)).saveError(eq(bulkOperationId), anyString(), anyString(), eq(ErrorType.ERROR)));

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
  @EnumSource(OperationStatusType.class)
  void shouldReturnBulkOperationById(OperationStatusType statusType) {
    var operationId = UUID.randomUUID();

    var bulkOperation = BulkOperation.builder()
      .id(operationId)
      .status(statusType)
      .totalNumOfRecords(10)
      .processedNumOfRecords(0)
      .build();

    var experctedBulkOperation = BulkOperation.builder()
      .id(operationId)
      .status(statusType)
      .totalNumOfRecords(10)
      .processedNumOfRecords(5)
      .build();

    when(bulkOperationRepository.findById(operationId))
      .thenReturn(Optional.of(bulkOperation));

    when(bulkOperationRepository.save(experctedBulkOperation))
      .thenReturn(experctedBulkOperation);

    when(dataProcessingRepository.findById(operationId))
      .thenReturn(Optional.of(BulkOperationDataProcessing.builder()
        .status(StatusType.ACTIVE)
        .processedNumOfRecords(5)
        .build()));

    when(executionRepository.findByBulkOperationId(operationId))
      .thenReturn(Optional.of(BulkOperationExecution.builder()
        .status(StatusType.ACTIVE)
        .processedRecords(5)
        .build()));

    when(metadataProviderService.getJobExecutions(operationId))
      .thenReturn(Collections.emptyList());

    when(metadataProviderService.calculateProgress(anyList()))
      .thenReturn(new DataImportProgress().total(10).current(5));

    when(queryService.checkQueryExecutionStatus(any(BulkOperation.class)))
      .thenReturn(experctedBulkOperation);

    var operation = bulkOperationService.getOperationById(operationId);

    if (Set.of(DATA_MODIFICATION, EXECUTING_QUERY, APPLY_CHANGES, APPLY_MARC_CHANGES).contains(operation.getStatus())) {
      assertThat(operation.getProcessedNumOfRecords(), equalTo(5));
    } else {
      assertThat(operation.getProcessedNumOfRecords(), equalTo(0));
    }
  }

  @Test
  void clearOperationProcessing() {
    var operationId = UUID.randomUUID();
    var operation = BulkOperation.builder()
      .id(operationId)
      .build();

    when(dataProcessingRepository.findById(operationId))
      .thenReturn(Optional.of(BulkOperationDataProcessing.builder()
        .bulkOperationId(operationId)
        .status(StatusType.ACTIVE)
        .processedNumOfRecords(5)
        .build()));

    bulkOperationService.clearOperationProcessing(operation);

    verify(dataProcessingRepository).deleteAllByBulkOperationId(any(UUID.class));
    verify(bulkOperationRepository).save(any(BulkOperation.class));
  }

  @ParameterizedTest
  @EnumSource(value = OperationStatusType.class, names = {"NEW", "RETRIEVING_RECORDS", "SAVING_RECORDS_LOCALLY"})
  void shouldRemoveTriggeringAndMatchedRecordsFilesOnCancel(OperationStatusType type) {
    var operationId = UUID.randomUUID();
    var linkToTriggeringCsv = "identifiers.csv";
    var linkToMatchedCsv = "matched.csv";
    var linkToMatchedJson = "matched.json";
    var linkToMatchedErrorsCsv = "matched-errors.csv";

    when(bulkOperationRepository.findById(operationId))
      .thenReturn(Optional.of(BulkOperation.builder()
        .id(operationId)
        .linkToTriggeringCsvFile(linkToTriggeringCsv)
        .linkToMatchedRecordsCsvFile(linkToMatchedCsv)
        .linkToMatchedRecordsJsonFile(linkToMatchedJson)
        .linkToMatchedRecordsErrorsCsvFile(linkToMatchedErrorsCsv)
        .status(type)
        .build()));

    bulkOperationService.cancelOperationById(operationId);

    verify(remoteFileSystemClient).remove(linkToTriggeringCsv);
    verify(remoteFileSystemClient).remove(linkToMatchedCsv);
    verify(remoteFileSystemClient).remove(linkToMatchedJson);
    verify(remoteFileSystemClient).remove(linkToMatchedErrorsCsv);
  }

  @ParameterizedTest
  @EnumSource(value = OperationStatusType.class, names = {"DATA_MODIFICATION", "REVIEW_CHANGES"})
  void shouldRemoveModifiedRecordsFilesOnCancel(OperationStatusType type) {
    var operationId = UUID.randomUUID();
    var linkToModifiedCsv = "modified.csv";
    var linkToModifiedJson = "modified.json";

    when(bulkOperationRepository.findById(operationId))
      .thenReturn(Optional.of(BulkOperation.builder()
        .id(operationId)
        .status(type)
        .approach(ApproachType.MANUAL)
        .linkToModifiedRecordsCsvFile(linkToModifiedCsv)
        .linkToModifiedRecordsJsonFile(linkToModifiedJson)
        .build()));

    bulkOperationService.cancelOperationById(operationId);

    verify(remoteFileSystemClient).remove(linkToModifiedCsv);
    verify(remoteFileSystemClient).remove(linkToModifiedJson);
  }

  @ParameterizedTest
  @EnumSource(value = OperationStatusType.class, names = {"NEW", "RETRIEVING_RECORDS", "SAVING_RECORDS_LOCALLY", "DATA_MODIFICATION", "REVIEW_CHANGES"}, mode = EnumSource.Mode.EXCLUDE)
  void shouldThrowExceptionOnInvalidStatusForCancel(OperationStatusType type) {
    var operationId = UUID.randomUUID();

    when(bulkOperationRepository.findById(operationId))
      .thenReturn(Optional.of(BulkOperation.builder()
        .id(operationId)
        .status(type)
        .build()));

    assertThrows(IllegalOperationStateException.class, () -> bulkOperationService.cancelOperationById(operationId));
  }

  @ParameterizedTest
  @EnumSource(DiscoverySuppressTestData.class)
  @SneakyThrows
  void shouldUpdateItemsWhenCommittingHoldingsRecordDiscoverySuppressed(DiscoverySuppressTestData testData) {
    var triggeringFileName = "identifiers.csv";
    var matchedRecordsJsonFileName = "matched.json";
    var modifiedRecordsJsonFileName = "modified.json";
    var operationId = UUID.randomUUID();
    var operation = BulkOperation.builder()
      .id(operationId)
      .linkToTriggeringCsvFile(triggeringFileName)
      .linkToMatchedRecordsJsonFile(matchedRecordsJsonFileName)
      .linkToModifiedRecordsJsonFile(modifiedRecordsJsonFileName)
      .entityType(HOLDINGS_RECORD)
      .identifierType(IdentifierType.ID)
      .build();
    var holdingsId = UUID.randomUUID().toString();
    var originalHoldingsString = objectMapper.writeValueAsString(ExtendedHoldingsRecord.builder().entity(HoldingsRecord.builder()
        .id(holdingsId)
      .discoverySuppress(testData.originalHoldingsDiscoverySuppress).build()).build());
    var modifiedHoldingsString = objectMapper.writeValueAsString(ExtendedHoldingsRecord.builder().entity(HoldingsRecord.builder()
        .id(holdingsId)
      .discoverySuppress(testData.modifiedHoldingsDiscoverySuppress).build()).build());
    when(bulkOperationRepository.save(any()))
      .thenReturn(operation);
    when(remoteFileSystemClient.get(matchedRecordsJsonFileName))
      .thenReturn(new ByteArrayInputStream(originalHoldingsString.getBytes()));
    when(remoteFileSystemClient.get(modifiedRecordsJsonFileName))
      .thenReturn(new ByteArrayInputStream(modifiedHoldingsString.getBytes()));
    when(remoteFileSystemClient.writer(anyString()))
      .thenReturn(new StringWriter());
    doNothing().when(errorService).saveError(any(), any(), any(), eq(ErrorType.ERROR));
    when(ruleService.getRules(operationId))
      .thenReturn(new BulkOperationRuleCollection()
        .bulkOperationRules(List.of(new BulkOperationRule()
          .ruleDetails(new BulkOperationRuleRuleDetails()
            .option(UpdateOptionType.SUPPRESS_FROM_DISCOVERY)
            .actions(List.of(new Action()
              .type(testData.actionType)
              .parameters(Collections.singletonList(new Parameter()
                .key(APPLY_TO_ITEMS)
                .value("true")))))))));
    when(itemClient.getByQuery(anyString(), anyInt()))
      .thenReturn(ItemCollection.builder()
        .items(List.of(
          Item.builder()
            .id(UUID.randomUUID().toString())
            .discoverySuppress(testData.item1DiscoverySuppress)
            .build(),
          Item.builder()
            .id(UUID.randomUUID().toString())
            .discoverySuppress(testData.item2DiscoverySuppress)
            .build()))
        .build());
    when(executionRepository.save(any()))
      .thenReturn(new BulkOperationExecution());

    doNothing().when(permissionsValidator).checkIfBulkEditWritePermissionExists(anyString(), any(), anyString());

    bulkOperationService.commit(operation);

    if (testData.expectedNumOfItemUpdates > 0) {
      verify(itemClient, times(testData.expectedNumOfItemUpdates)).updateItem(any(), anyString());
    }
    if (nonNull(testData.expectedErrorMessage)) {
      verify(errorService).saveError(any(), any(), eq(testData.expectedErrorMessage), eq(ErrorType.WARNING));
    }
  }

  @Test
  void shouldCheckQueryExecution() {
    var operationId = UUID.randomUUID();
    var operation = new BulkOperation();
    operation.setStatus(EXECUTING_QUERY);
    when(bulkOperationRepository.findById(operationId)).thenReturn(Optional.of(operation));

    bulkOperationService.getOperationById(operationId);

    verify(queryService).checkQueryExecutionStatus(operation);
  }

  @Test
  void shouldStartDataExportJobWhenIdentifiersWereSaved() {
    var operationId = UUID.randomUUID();
    var operation = new BulkOperation();
    operation.setId(operationId);
    operation.setStatus(SAVED_IDENTIFIERS);
    operation.setEntityType(ITEM);
    when(bulkOperationRepository.findById(operationId)).thenReturn(Optional.of(operation));

    bulkOperationService.getOperationById(operationId);

    verify(dataExportSpringClient).upsertJob(any(Job.class));
  }

  @ParameterizedTest
  @EnumSource(OperationStatusType.class)
  @SneakyThrows
  void shouldWriteBeanToCsvAfterConverterException(OperationStatusType operationStatusType) {
    var item = Item.builder()
      .id(UUID.randomUUID().toString())
      .barcode("barcode")
      .statisticalCodes(Collections.singletonList(UUID.randomUUID().toString()))
      .build();

    var operation = BulkOperation.builder()
      .id(UUID.randomUUID())
      .identifierType(IdentifierType.BARCODE)
      .status(operationStatusType)
      .build();

    when(itemReferenceService.getStatisticalCodeById(anyString()))
      .thenThrow(new NotFoundException("not found"));

    try (var stringWriter = new StringWriter()) {
      var writer = new BulkOperationsEntityCsvWriter(stringWriter, Item.class);
      List<BulkOperationExecutionContent> bulkOperationExecutionContents = new ArrayList<>();
      bulkOperationService.writeBeanToCsv(operation, writer, item, bulkOperationExecutionContents);
      assertThat(stringWriter.toString(), containsString("FAILED"));
      if (APPLY_CHANGES.equals(operation.getStatus())) {
        assertThat(bulkOperationExecutionContents, hasSize(0));
      } else {
        assertThat(bulkOperationExecutionContents, hasSize(1));
        assertThat(bulkOperationExecutionContents.get(0).getBulkOperationId(), equalTo(operation.getId()));
        assertThat(bulkOperationExecutionContents.get(0).getState(), equalTo(StateType.FAILED));
      }
    }
  }

  @Test
  void shouldProcessUpdateInConsortia() {
    Map<String, Collection<String>> headers = new HashMap<>();
    headers.put("tenant", List.of("central"));

    var item = Item.builder()
      .id(UUID.randomUUID().toString())
      .barcode("barcode")
      .statisticalCodes(Collections.singletonList(UUID.randomUUID().toString()))
      .build();
    var extendedItem = ExtendedItem.builder().tenantId("member").entity(item).build();

    var operation = BulkOperation.builder()
      .id(UUID.randomUUID())
      .identifierType(IdentifierType.BARCODE)
      .build();

    var rules = new BulkOperationRule()
      .bulkOperationId(operation.getId())
      .ruleDetails(new BulkOperationRuleRuleDetails()
        .option(UpdateOptionType.SUPPRESS_FROM_DISCOVERY)
        .actions(List.of(new Action()
          .type(UpdateActionType.SET_TO_TRUE))));
    var rulesCollection = new BulkOperationRuleCollection().bulkOperationRules(List.of(rules)).totalRecords(1);

    when(consortiaService.isTenantCentral(any())).thenReturn(true);

    try (var context =  new FolioExecutionContextSetter(folioExecutionContext)) {
      var modified = bulkOperationService.processUpdate(extendedItem, operation, rulesCollection, ExtendedItem.class);
      var itemEntity = (Item) modified.getUpdated().getRecordBulkOperationEntity();

      assertTrue(itemEntity.getDiscoverySuppress());
    }
  }

  @Test
  void shouldProcessDataImportResult() {
    try (var context =  new FolioExecutionContextSetter(folioExecutionContext)) {
      UUID dataImportJobProfileId = UUID.randomUUID();
      BulkOperation operation = BulkOperation.builder()
        .id(UUID.randomUUID())
        .dataImportJobProfileId(dataImportJobProfileId)
        .build();

      when(metadataProviderService.getJobExecutions(dataImportJobProfileId)).thenReturn(List.of(new DataImportJobExecution().status(DataImportStatus.COMMITTED)));
      when(metadataProviderService.calculateProgress(any())).thenReturn(new DataImportProgress().current(10));
      when(metadataProviderService.isDataImportJobCompleted(any())).thenReturn(true);
      when(metadataProviderService.fetchUpdatedInstanceIds(any())).thenReturn(List.of(UUID.randomUUID().toString()));

      bulkOperationService.processDataImportResult(operation);

      verify(metadataProviderService).getJobExecutions(dataImportJobProfileId);
      verify(metadataProviderService).calculateProgress(any());
      verify(metadataProviderService).isDataImportJobCompleted(any());
      verify(metadataProviderService).fetchUpdatedInstanceIds(any());
    }
  }

  @Test
  @SneakyThrows
  void shouldSkipUnsupportedInstancesOnConfirm() {
    var operationId = UUID.randomUUID();
    var matchedCsvFileName = "matched.csv";
    var matchedJsonFileName = "matched.json";
    var operation = BulkOperation.builder()
      .id(operationId)
      .entityType(INSTANCE_MARC)
      .linkToTriggeringCsvFile("instances.csv")
      .linkToMatchedRecordsCsvFile(matchedCsvFileName)
      .build();
    var processing = BulkOperationDataProcessing.builder()
      .bulkOperationId(operationId)
      .build();

    when(remoteFileSystemClient.get(matchedCsvFileName))
      .thenReturn(new FileInputStream("src/test/resources/files/instance.csv"));
    when(remoteFileSystemClient.get(matchedJsonFileName))
      .thenReturn(new FileInputStream("src/test/resources/files/extended_instance.json"));
    when(bulkOperationRepository.findById(operationId))
      .thenReturn(Optional.of(operation));

    bulkOperationService.confirm(processing);

    verify(bulkOperationRepository, times(0)).save(any(BulkOperation.class));
  }

  @Test
  void shouldSkipConfirmForInstanceMarcIfNoMatchedRecordsMarcFile() {
    var operationId = UUID.randomUUID();
    var operation = BulkOperation.builder()
      .id(operationId)
      .entityType(INSTANCE_MARC)
      .build();
    var processing = BulkOperationDataProcessing.builder()
      .bulkOperationId(operationId)
      .build();

    when(bulkOperationRepository.findById(operationId))
      .thenReturn(Optional.of(operation));

    bulkOperationService.confirmForInstanceMarc(processing);

    var processingCaptor = ArgumentCaptor.forClass(BulkOperationDataProcessing.class);
    verify(dataProcessingRepository).save(processingCaptor.capture());
    assertEquals(StatusType.FAILED, processingCaptor.getValue().getStatus());
  }

  @Test
  @SneakyThrows
  void shouldSkipConfirmForInstanceMarkIfNoRules() {
    var operationId = UUID.randomUUID();
    var matchedMrcFileName = "matched.mrc";
    var operation = BulkOperation.builder()
      .id(operationId)
      .linkToTriggeringCsvFile("instances.csv")
      .linkToMatchedRecordsMarcFile(matchedMrcFileName)
      .entityType(INSTANCE_MARC)

      .build();
    var processing = BulkOperationDataProcessing.builder()
      .bulkOperationId(operationId)
      .build();

    when(bulkOperationRepository.findById(operationId))
      .thenReturn(Optional.of(operation));
    when(ruleService.getMarcRules(operationId))
      .thenReturn(new BulkOperationMarcRuleCollection());
    when(remoteFileSystemClient.get(matchedMrcFileName))
      .thenReturn(new FileInputStream("src/test/resources/files/matched.mrc"));

    bulkOperationService.confirmForInstanceMarc(processing);

    verify(marcInstanceDataProcessor, never()).update(any(BulkOperation.class), any(Record.class),
      any(org.folio.bulkops.domain.dto.BulkOperationMarcRuleCollection.class), any(Date.class));
  }

  @Test
  void shouldSkipCommitIfNoRules() {
    var operationId = UUID.randomUUID();
    var operation = BulkOperation.builder()
      .id(operationId)
      .entityType(INSTANCE_MARC)
      .linkToTriggeringCsvFile("instances.csv")
      .linkToModifiedRecordsCsvFile("modified.csv")
      .linkToModifiedRecordsMarcFile("modified.mrc")
      .build();

    when(bulkOperationRepository.save(any(BulkOperation.class)))
      .thenReturn(operation);
    when(ruleService.hasAdministrativeUpdates(operation))
      .thenReturn(false);
    when(ruleService.hasMarcUpdates(operation))
      .thenReturn(false);

    bulkOperationService.commit(operation);

    var operationCaptor = ArgumentCaptor.forClass(BulkOperation.class);
    verify(bulkOperationRepository, times(2)).save(operationCaptor.capture());
    var lastCapture = operationCaptor.getAllValues().get(1);
    assertNull(lastCapture.getLinkToCommittedRecordsMarcFile());
    assertNull(lastCapture.getLinkToCommittedRecordsCsvFile());
  }

  @ParameterizedTest
  @CsvSource(textBlock = """
    true  | false
    false | true
    """, delimiter = '|')
  void shouldStartCommitInstanceMarcIfRulesArePresent(boolean hasAdministrativeRules, boolean hasMarcRules) {
    var operationId = UUID.randomUUID();
    var operation = BulkOperation.builder()
      .id(operationId)
      .entityType(INSTANCE_MARC)
      .linkToTriggeringCsvFile("instances.csv")
      .linkToModifiedRecordsJsonFile("modified.json")
      .build();

    when(executionRepository.save(any(BulkOperationExecution.class)))
      .thenReturn(new BulkOperationExecution());
    when(bulkOperationRepository.save(any(BulkOperation.class)))
      .thenReturn(operation);
    when(ruleService.hasAdministrativeUpdates(operation))
      .thenReturn(hasAdministrativeRules);
    when(ruleService.hasMarcUpdates(operation))
      .thenReturn(hasMarcRules);

    bulkOperationService.commit(operation);

    if (hasAdministrativeRules) {
      verify(executionRepository, times(2)).save(any(BulkOperationExecution.class));
    }
    if (hasMarcRules) {
      verify(marcUpdateService).commitForInstanceMarc(any(BulkOperation.class));
    }
  }
}
