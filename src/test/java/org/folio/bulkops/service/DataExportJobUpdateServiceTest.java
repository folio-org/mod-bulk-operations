package org.folio.bulkops.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import lombok.SneakyThrows;
import org.folio.bulkops.BaseTest;
import org.folio.bulkops.client.RemoteFileSystemClient;
import org.folio.bulkops.domain.bean.BatchStatus;
import org.folio.bulkops.domain.bean.Job;
import org.folio.bulkops.domain.dto.ApproachType;
import org.folio.bulkops.domain.dto.OperationStatusType;
import org.folio.bulkops.domain.bean.Progress;
import org.folio.bulkops.domain.entity.BulkOperation;
import org.folio.bulkops.repository.BulkOperationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

class DataExportJobUpdateServiceTest extends BaseTest {
  @Autowired
  private DataExportJobUpdateService dataExportJobUpdateService;

  @MockBean
  private BulkOperationRepository bulkOperationRepository;
  @MockBean
  private RemoteFileSystemClient remoteFileSystemClient;

  @ParameterizedTest
  @EnumSource(value = ApproachType.class, names = {"QUERY", "IN_APP" }, mode = EnumSource.Mode.INCLUDE)
  @SneakyThrows
  void shouldUpdateBulkOperationForCompletedJob(ApproachType approach) {
    var bulkOperationId = UUID.randomUUID();
    var jobId = UUID.randomUUID();
    when(bulkOperationRepository.findByDataExportJobId(jobId))
      .thenReturn(Optional.of(BulkOperation.builder()
        .id(bulkOperationId)
        .approach(approach)
        .build()));

    var expectedCsvErrorsFileName = bulkOperationId + "/errors.csv";

    var expectedCsvFileName = bulkOperationId + "/users.csv";
    when(remoteFileSystemClient.put(any(InputStream.class), eq(expectedCsvFileName)))
      .thenReturn(expectedCsvFileName);

    var expectedJsonFileName = bulkOperationId + "/user.json";
    when(remoteFileSystemClient.put(any(InputStream.class), eq(expectedJsonFileName)))
      .thenReturn(expectedJsonFileName);

    when(remoteFileSystemClient.put(any(InputStream.class), eq(expectedCsvErrorsFileName)))
      .thenReturn(expectedCsvErrorsFileName);

    when(remoteFileSystemClient.getNumOfLines(eq(expectedCsvFileName)))
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
      .files(List.of("file:src/test/resources/files/users.csv", "file:src/test/resources/files/errors.csv", "file:src/test/resources/files/user.json")).build();

    dataExportJobUpdateService.receiveJobExecutionUpdate(jobUpdate);

    var operationCaptor = ArgumentCaptor.forClass(BulkOperation.class);
    verify(bulkOperationRepository, times(2)).save(operationCaptor.capture());
    assertEquals(OperationStatusType.SAVING_RECORDS_LOCALLY, operationCaptor.getAllValues().get(0).getStatus());
    assertEquals(OperationStatusType.DATA_MODIFICATION, operationCaptor.getAllValues().get(1).getStatus());
    assertEquals(expectedJsonFileName, operationCaptor.getAllValues().get(1).getLinkToMatchedRecordsJsonFile());
    assertEquals(expectedCsvFileName, operationCaptor.getAllValues().get(1).getLinkToMatchedRecordsCsvFile());
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
    dataExportJobUpdateService.receiveJobExecutionUpdate(Job.builder()
      .id(jobId)
      .batchStatus(batchStatus)
      .progress(Progress.builder()
        .total(totalRecords)
        .processed(processedRecords).build()).build());

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

    dataExportJobUpdateService.receiveJobExecutionUpdate(Job.builder()
      .id(jobId)
      .batchStatus(batchStatus)
      .endTime(endTime)
      .progress(Progress.builder()
        .total(totalRecords)
        .processed(processedRecords).build()).build());

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

    dataExportJobUpdateService.receiveJobExecutionUpdate(Job.builder().id(UUID.randomUUID()).build());

    verify(bulkOperationRepository, times(0)).save(any(BulkOperation.class));
  }
}
