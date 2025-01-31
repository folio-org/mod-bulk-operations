package org.folio.bulkops.service;

import static org.folio.bulkops.util.ErrorCode.ERROR_NOT_DOWNLOAD_ORIGIN_FILE_FROM_S3;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.FileInputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import lombok.SneakyThrows;
import org.folio.bulkops.BaseTest;
import org.folio.bulkops.client.RemoteFileSystemClient;
import org.folio.bulkops.domain.bean.BatchStatus;
import org.folio.bulkops.domain.bean.Job;
import org.folio.bulkops.domain.bean.Progress;
import org.folio.bulkops.domain.dto.ApproachType;
import org.folio.bulkops.domain.dto.EntityType;
import org.folio.bulkops.domain.dto.OperationStatusType;
import org.folio.bulkops.domain.entity.BulkOperation;
import org.folio.bulkops.exception.NotFoundException;
import org.folio.bulkops.exception.ServerErrorException;
import org.folio.bulkops.repository.BulkOperationRepository;
import org.folio.s3.exception.S3ClientException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

class DataExportJobUpdateServiceTest extends BaseTest {
  @Autowired
  private DataExportJobUpdateReceiverService dataExportJobUpdateReceiverService;
  @Autowired
  private DataExportJobUpdateService dataExportJobUpdateService;

  @MockBean
  private BulkOperationRepository bulkOperationRepository;
  @MockBean
  private RemoteFileSystemClient remoteFileSystemClient;

  @ParameterizedTest
  @EnumSource(value = ApproachType.class, names = {"IN_APP" }, mode = EnumSource.Mode.INCLUDE)
  @SneakyThrows
  void shouldUpdateBulkOperationForCompletedJob(ApproachType approach) {
    var bulkOperationId = UUID.randomUUID();
    var jobId = UUID.randomUUID();

    when(bulkOperationRepository.findByDataExportJobId(jobId))
      .thenReturn(Optional.of(BulkOperation.builder()
        .id(bulkOperationId)
        .entityType(EntityType.USER)
        .approach(approach)
        .build()));

    var expectedCsvErrorsFileName = bulkOperationId + "/errors.csv";

    var expectedCsvFileName = bulkOperationId + "/users.csv";
    when(remoteFileSystemClient.put(any(InputStream.class), eq(expectedCsvFileName)))
      .thenReturn(expectedCsvFileName);

    var expectedJsonFileName = bulkOperationId + "/json/user.json";
    when(remoteFileSystemClient.put(any(InputStream.class), eq(expectedJsonFileName)))
      .thenReturn(expectedJsonFileName);

    var expectedMarcFileName = bulkOperationId + "/preview.mrc";
    when(remoteFileSystemClient.put(any(InputStream.class), eq(expectedMarcFileName)))
      .thenReturn(expectedMarcFileName);

    when(remoteFileSystemClient.put(any(InputStream.class), eq(expectedCsvErrorsFileName)))
      .thenReturn(expectedCsvErrorsFileName);

    when(remoteFileSystemClient.getNumOfLines(expectedCsvFileName))
      .thenReturn(3);

    var totalRecords = 10;
    var processedRecords = 10;

    var jobUpdate = Job.builder()
      .id(jobId)
      .batchStatus(BatchStatus.COMPLETED)
      .endTime(new Date())
      .progress(Progress.builder()
        .total(totalRecords)
        .processed(processedRecords).build())
      .files(List.of("file:src/test/resources/files/users.csv", "file:src/test/resources/files/errors.csv", "file:src/test/resources/files/user.json", "file:src/test/resources/files/preview.mrc")).build();

    dataExportJobUpdateReceiverService.receiveJobExecutionUpdate(jobUpdate, okapiHeaders);

    var operationCaptor = ArgumentCaptor.forClass(BulkOperation.class);
    verify(bulkOperationRepository, times(2)).save(operationCaptor.capture());
    assertEquals(OperationStatusType.DATA_MODIFICATION, operationCaptor.getAllValues().get(1).getStatus());
    assertEquals(expectedJsonFileName, operationCaptor.getAllValues().get(1).getLinkToMatchedRecordsJsonFile());
    assertEquals(expectedCsvFileName, operationCaptor.getAllValues().get(1).getLinkToMatchedRecordsCsvFile());
    assertEquals(expectedMarcFileName, operationCaptor.getAllValues().get(1).getLinkToMatchedRecordsMarcFile());
    assertEquals(expectedCsvErrorsFileName, operationCaptor.getAllValues().get(1).getLinkToMatchedRecordsErrorsCsvFile());
  }

  @ParameterizedTest
  @EnumSource(value = BatchStatus.class, names = { "STARTING", "STARTED", "STOPPING", "STOPPED" }, mode = EnumSource.Mode.INCLUDE)
  void shouldUpdateBulkOperationForJobInProgress(BatchStatus batchStatus) {
    var jobId = UUID.randomUUID();
    when(bulkOperationRepository.findByDataExportJobId(jobId))
      .thenReturn(Optional.of(BulkOperation.builder()
        .id(UUID.randomUUID())
        .build()));

    var totalRecords = 10;
    var processedRecords = 5;
    dataExportJobUpdateReceiverService.receiveJobExecutionUpdate(Job.builder()
      .id(jobId)
      .batchStatus(batchStatus)
      .progress(Progress.builder()
        .total(totalRecords)
        .processed(processedRecords).build()).build(), okapiHeaders);

    var operationCaptor = ArgumentCaptor.forClass(BulkOperation.class);
    verify(bulkOperationRepository).save(operationCaptor.capture());
    assertEquals(totalRecords, operationCaptor.getValue().getTotalNumOfRecords());
    assertEquals(processedRecords, operationCaptor.getValue().getProcessedNumOfRecords());
  }

  @ParameterizedTest
  @EnumSource(value = BatchStatus.class, names = { "FAILED", "ABANDONED" }, mode = EnumSource.Mode.INCLUDE)
  void shouldUpdateBulkOperationForFailedJob(BatchStatus batchStatus) {
    var jobId = UUID.randomUUID();
    when(bulkOperationRepository.findByDataExportJobId(jobId))
      .thenReturn(Optional.of(BulkOperation.builder()
        .id(UUID.randomUUID())
        .build()));

    var totalRecords = 10;
    var processedRecords = 5;
    var endTime = new Date();
    var expectedEndTime = LocalDateTime.ofInstant(endTime.toInstant(), ZoneId.of("UTC"));

    dataExportJobUpdateReceiverService.receiveJobExecutionUpdate(Job.builder()
      .id(jobId)
      .batchStatus(batchStatus)
      .endTime(endTime)
      .progress(Progress.builder()
        .total(totalRecords)
        .processed(processedRecords).build()).build(), okapiHeaders);

    var operationCaptor = ArgumentCaptor.forClass(BulkOperation.class);
    verify(bulkOperationRepository).save(operationCaptor.capture());
    assertEquals(totalRecords, operationCaptor.getValue().getTotalNumOfRecords());
    assertEquals(processedRecords, operationCaptor.getValue().getProcessedNumOfRecords());
    assertEquals(OperationStatusType.FAILED, operationCaptor.getValue().getStatus());
    assertEquals(expectedEndTime, operationCaptor.getValue().getEndTime());
  }

  @Test
  void shouldSkipUpdateForUnknownJob() {
    when(bulkOperationRepository.findByDataExportJobId(any(UUID.class)))
      .thenReturn(Optional.empty());

    dataExportJobUpdateReceiverService.receiveJobExecutionUpdate(Job.builder().id(UUID.randomUUID()).build(), okapiHeaders);

    verify(bulkOperationRepository, times(0)).save(any(BulkOperation.class));
  }

  @ParameterizedTest
  @MethodSource("provideProgressValues")
  void shouldSetDefaultValuesIfProgressIsEmptyOrNull(Progress progress) {
    var jobId = UUID.randomUUID();
    when(bulkOperationRepository.findByDataExportJobId(jobId))
      .thenReturn(Optional.of(BulkOperation.builder()
        .id(UUID.randomUUID())
        .build()));
    when(remoteFileSystemClient.put(any(InputStream.class), anyString()))
      .thenReturn("file.csv");

    var jobUpdate = Job.builder()
      .id(jobId)
      .files(List.of("file:src/test/resources/files/users.csv", "file:src/test/resources/files/errors.csv", "file:src/test/resources/files/user.json"))
      .batchStatus(BatchStatus.COMPLETED)
      .progress(progress)
      .build();

    dataExportJobUpdateService.handleReceivedJobExecutionUpdate(jobUpdate);

    var operationCaptor = ArgumentCaptor.forClass(BulkOperation.class);
    verify(bulkOperationRepository, times(2)).save(operationCaptor.capture());
    var operation = operationCaptor.getAllValues().get(1);
    assertEquals(0, operation.getTotalNumOfRecords());
    assertEquals(0, operation.getProcessedNumOfRecords());
    assertEquals(0, operation.getMatchedNumOfRecords());
    assertEquals(0, operation.getMatchedNumOfErrors());
  }

  @ParameterizedTest
  @MethodSource("provideBatchStatusValues")
  void shouldSetDefaultValuesIfJobStatusIsNullOrFailed(BatchStatus batchStatus) {
    var jobId = UUID.randomUUID();
    when(bulkOperationRepository.findByDataExportJobId(jobId))
      .thenReturn(Optional.of(BulkOperation.builder()
        .id(UUID.randomUUID())
        .build()));
    when(remoteFileSystemClient.put(any(InputStream.class), anyString()))
      .thenReturn("file.csv");

    var jobUpdate = Job.builder()
      .id(jobId)
      .files(List.of("file:src/test/resources/files/users.csv", "file:src/test/resources/files/errors.csv", "file:src/test/resources/files/user.json"))
      .batchStatus(batchStatus)
      .progress(Progress.builder().build())
      .endTime(new Date())
      .build();

    dataExportJobUpdateService.handleReceivedJobExecutionUpdate(jobUpdate);

    var operationCaptor = ArgumentCaptor.forClass(BulkOperation.class);
    verify(bulkOperationRepository, times(1)).save(operationCaptor.capture());
    var operation = operationCaptor.getAllValues().get(0);
    assertEquals(0, operation.getTotalNumOfRecords());
    assertEquals(0, operation.getProcessedNumOfRecords());
    assertEquals(0, operation.getMatchedNumOfRecords());
    assertEquals(0, operation.getMatchedNumOfErrors());
  }

  @Test
  void shouldThrowS3ExceptionIfS3ClientIssue() {
    var jobId = UUID.randomUUID();
    var bulkOperation = BulkOperation.builder().id(UUID.randomUUID()).build();
    when(bulkOperationRepository.findByDataExportJobId(jobId))
      .thenReturn(Optional.of(bulkOperation));
    doThrow(new S3ClientException("s3 issue")).when(remoteFileSystemClient).put(any(InputStream.class), anyString());

    var jobUpdate = Job.builder()
      .id(jobId)
      .files(List.of("file:src/test/resources/files/users.csv", "file:src/test/resources/files/errors.csv", "file:src/test/resources/files/user.json"))
      .batchStatus(BatchStatus.COMPLETED)
      .progress(Progress.builder().build())
      .build();

    dataExportJobUpdateService.handleReceivedJobExecutionUpdate(jobUpdate);

    assertEquals(ERROR_NOT_DOWNLOAD_ORIGIN_FILE_FROM_S3 + " : s3 issue", bulkOperation.getErrorMessage());
  }

  @Test
  @SneakyThrows
  void shouldDownloadAndSaveJsonFileForItemEntityType() {
    var bulkOperation = BulkOperation.builder()
      .id(UUID.randomUUID())
      .entityType(EntityType.ITEM)
      .build();
    var jobUpdate = Job.builder()
      .files(List.of("file:fake.csv", "file:fake.csv", "file:src/test/resources/files/item.json", "file:fake.mrc"))
      .build();

    var jsonFilePath = bulkOperation.getId() + "/json/item.json";
    when(remoteFileSystemClient.put(any(InputStream.class), eq(jsonFilePath)))
      .thenReturn(jsonFilePath);
    when(remoteFileSystemClient.get(jsonFilePath))
      .thenReturn(new FileInputStream("src/test/resources/files/item.json"));

    var bulkOperationCaptor = ArgumentCaptor.forClass(BulkOperation.class);

    var result = dataExportJobUpdateService.downloadAndSaveJsonFile(bulkOperation, jobUpdate);

    assertEquals(jsonFilePath, result);
    verify(bulkOperationRepository, times(1)).save(bulkOperationCaptor.capture());
    var savedBulkOperation = bulkOperationCaptor.getValue();
    Assertions.assertNotNull(savedBulkOperation.getUsedTenants());
    Assertions.assertFalse(savedBulkOperation.getUsedTenants().isEmpty());
    Assertions.assertEquals(1, savedBulkOperation.getUsedTenants().size());
    Assertions.assertEquals("member", savedBulkOperation.getUsedTenants().get(0));
  }

  @Test
  @SneakyThrows
  void shouldFailToDownloadAndSaveJsonFile() {

    var bulkOperation = BulkOperation.builder()
      .id(UUID.randomUUID())
      .entityType(EntityType.ITEM)
      .build();

    var jobUpdate = Job.builder()
      .files(List.of("file:fake.csv", "file:fake.csv", "file:src/test/resources/files/item.json", "file:fake.mrc"))
      .build();

    var jsonFilePath = bulkOperation.getId() + "/json/item.json";
    when(remoteFileSystemClient.put(any(InputStream.class), eq(jsonFilePath)))
      .thenReturn(jsonFilePath);
    when(remoteFileSystemClient.get(jsonFilePath))
      .thenThrow(new NotFoundException("Failed to download file"));

    assertThrows(ServerErrorException.class, () -> dataExportJobUpdateService.downloadAndSaveJsonFile(bulkOperation, jobUpdate));
    verify(bulkOperationRepository, times(1)).save(any(BulkOperation.class));
  }

  @Test
  @SneakyThrows
  void shouldReturnNullWhenMarcUrlIsEmpty() {
    assertNull(dataExportJobUpdateService.downloadAndSaveMarcFile(new BulkOperation(), new Job().withFiles(List.of("", "", "", ""))));
  }

  private static Stream<Progress> provideProgressValues() {
    return Stream.of(new Progress[] {Progress.builder().build(), null});
  }

  private static Stream<BatchStatus> provideBatchStatusValues() {
    return Stream.of(new BatchStatus[] {BatchStatus.FAILED, null});
  }
}
