package org.folio.bulkops.service;

import lombok.SneakyThrows;
import org.folio.bulkops.BaseTest;
import org.folio.bulkops.domain.bean.BatchStatus;
import org.folio.bulkops.domain.bean.Job;
import org.folio.bulkops.domain.bean.Progress;
import org.folio.bulkops.domain.dto.ApproachType;
import org.folio.bulkops.domain.dto.OperationStatusType;
import org.folio.bulkops.domain.entity.BulkOperation;
import org.folio.bulkops.repository.BulkOperationRepository;
import org.folio.s3.client.FolioS3Client;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DataExportJobUpdateServiceTest extends BaseTest {
  @Autowired
  private DataExportJobUpdateService dataExportJobUpdateService;

  @Autowired
  private BulkOperationRepository bulkOperationRepository;

  @Autowired
  private FolioS3Client localFolioS3Client;

  @ParameterizedTest
  @EnumSource(value = ApproachType.class, names = {"QUERY", "IN_APP" }, mode = EnumSource.Mode.INCLUDE)
  @SneakyThrows
  void shouldUpdateBulkOperationForCompletedJob(ApproachType approach) {
    var bulkOperationId = UUID.randomUUID();
    var jobId = UUID.randomUUID();

    bulkOperationRepository.save(BulkOperation.builder()
      .id(bulkOperationId)
        .dataExportJobId(jobId)
      .approach(approach)
      .build());

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

    var bulkOperation = bulkOperationRepository.findById(bulkOperationId).get();

    assertEquals(OperationStatusType.DATA_MODIFICATION, bulkOperation.getStatus());
    assertEquals(bulkOperationId + "/user.json", bulkOperation.getLinkToMatchedRecordsJsonFile());
    assertEquals(bulkOperationId + "/users.csv", bulkOperation.getLinkToMatchedRecordsCsvFile());
    assertEquals(bulkOperationId + "/errors.csv", bulkOperation.getLinkToMatchedRecordsErrorsCsvFile());
  }

  @ParameterizedTest
  @EnumSource(value = BatchStatus.class, names = { "STARTING", "STARTED", "STOPPING", "STOPPED" }, mode = EnumSource.Mode.INCLUDE)
  void shouldUpdateBulkOperationForJobInProgress(BatchStatus batchStatus) {
    var bulkOperationId = UUID.randomUUID();
    var jobId = UUID.randomUUID();

    bulkOperationRepository.save(BulkOperation.builder()
      .id(bulkOperationId)
      .dataExportJobId(jobId)
      .build());

    var totalRecords = 10;
    var processedRecords = 5;
    dataExportJobUpdateService.receiveJobExecutionUpdate(Job.builder()
      .id(jobId)
        .batchStatus(batchStatus)
      .progress(Progress.builder()
        .total(totalRecords)
        .processed(processedRecords).build()).build());

    var bulkOperation = bulkOperationRepository.findById(bulkOperationId).get();
    assertEquals(totalRecords, bulkOperation.getTotalNumOfRecords());
    assertEquals(processedRecords, bulkOperation.getProcessedNumOfRecords());
  }

  @ParameterizedTest
  @EnumSource(value = BatchStatus.class, names = { "FAILED", "ABANDONED" }, mode = EnumSource.Mode.INCLUDE)
  void shouldUpdateBulkOperationForFailedJob(BatchStatus batchStatus) {
    var bulkOperationId = UUID.randomUUID();
    var jobId = UUID.randomUUID();

    bulkOperationRepository.save(BulkOperation.builder()
      .id(bulkOperationId)
      .dataExportJobId(jobId)
      .build());

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

    var bulkOperation = bulkOperationRepository.findById(bulkOperationId).get();
    assertEquals(totalRecords, bulkOperation.getTotalNumOfRecords());
    assertEquals(processedRecords, bulkOperation.getProcessedNumOfRecords());
    assertEquals(OperationStatusType.FAILED, bulkOperation.getStatus());
    assertEquals(expectedEndTime, bulkOperation.getEndTime());
  }

  @Test
  void shouldSkipUpdateForUnknownJob() {
    var jobId = UUID.randomUUID();
    dataExportJobUpdateService.receiveJobExecutionUpdate(Job.builder().id(jobId).build());
    Assertions.assertTrue(bulkOperationRepository.findByDataExportJobId(jobId).isEmpty());
  }
}
