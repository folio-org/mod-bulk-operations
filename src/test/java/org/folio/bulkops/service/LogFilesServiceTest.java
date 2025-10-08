package org.folio.bulkops.service;

import static org.folio.bulkops.service.LogFilesService.CSV_PATH_TEMPLATE;
import static org.folio.bulkops.service.LogFilesService.JSON_PATH_TEMPLATE;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.UUID;
import org.folio.bulkops.BaseTest;
import org.folio.bulkops.domain.entity.BulkOperation;
import org.folio.bulkops.repository.BulkOperationRepository;
import org.folio.s3.exception.S3ClientException;
import org.folio.spring.scope.FolioExecutionContextSetter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.testcontainers.shaded.org.apache.commons.io.IOUtils;

class LogFilesServiceTest extends BaseTest {

  @Autowired
  private LogFilesService logFilesService;

  @Autowired
  private BulkOperationRepository bulkOperationRepository;

  private UUID oldBulkOperationId;
  private UUID nonOldBulkOperationId;

  @BeforeEach()
  void saveBulkOperation() {
    var back31days = LocalDateTime.now().minusDays(31);
    var back29days = LocalDateTime.now().minusDays(29);
    try (var ignored = new FolioExecutionContextSetter(folioExecutionContext)) {
      oldBulkOperationId = bulkOperationRepository.save(BulkOperation.builder()
        .id(UUID.randomUUID())
        .linkToTriggeringCsvFile("some/path/records.csv")
        .linkToMatchedRecordsJsonFile("some/path/records.csv")
        .linkToMatchedRecordsMarcFile("some/path/records.mrc")
        .linkToModifiedRecordsCsvFile("some/path/records.csv")
        .linkToModifiedRecordsMarcFile("some/path/records.mrc")
        .linkToModifiedRecordsMarcCsvFile("some/path/records.csv")
        .linkToModifiedRecordsJsonFile("some/path/records.csv")
        .linkToPreviewRecordsJsonFile("some/path/records.csv")
        .linkToCommittedRecordsJsonFile("some/path/records.csv")
        .linkToMatchedRecordsCsvFile("some/path/records.csv")
        .linkToMatchedRecordsErrorsCsvFile("some/path/records.csv")
        .linkToCommittedRecordsErrorsCsvFile("some/path/records.csv")
        .linkToCommittedRecordsCsvFile("some/path/records.csv")
        .linkToCommittedRecordsMarcFile("some/path/records.mrc")
        .linkToCommittedRecordsMarcCsvFile("some/path/records.csv")
        .endTime(back31days)
        .build()).getId();
      nonOldBulkOperationId = bulkOperationRepository.save(BulkOperation.builder()
        .id(UUID.randomUUID())
        .linkToTriggeringCsvFile("some/path/records.csv")
        .linkToMatchedRecordsJsonFile("some/path/records.csv")
        .linkToMatchedRecordsMarcFile("some/path/records.mrc")
        .linkToModifiedRecordsCsvFile("some/path/records.csv")
        .linkToModifiedRecordsMarcFile("some/path/records.mrc")
        .linkToModifiedRecordsMarcCsvFile("some/path/records.csv")
        .linkToModifiedRecordsJsonFile("some/path/records.csv")
        .linkToPreviewRecordsJsonFile("some/path/records.csv")
        .linkToCommittedRecordsJsonFile("some/path/records.csv")
        .linkToMatchedRecordsCsvFile("some/path/records.csv")
        .linkToMatchedRecordsErrorsCsvFile("some/path/records.csv")
        .linkToCommittedRecordsErrorsCsvFile("some/path/records.csv")
        .linkToCommittedRecordsCsvFile("some/path/records.csv")
        .linkToCommittedRecordsMarcFile("some/path/records.mrc")
        .linkToCommittedRecordsMarcCsvFile("some/path/records.csv")
        .endTime(back29days)
        .build()).getId();
    }
  }

  @AfterEach
  void clearTestData() {
    try (var ignored =  new FolioExecutionContextSetter(folioExecutionContext)) {
      bulkOperationRepository.deleteById(oldBulkOperationId);
      bulkOperationRepository.deleteById(nonOldBulkOperationId);
    }
  }

  @Test
  void shouldClearFilesAndMakeOperationExpiredOnlyOfOldBulkOperations() {
    try (var ignored = new FolioExecutionContextSetter(folioExecutionContext)) {
      var oldOperation = bulkOperationRepository.findById(oldBulkOperationId).get();
      var nonOldOperation = bulkOperationRepository.findById(nonOldBulkOperationId).get();
      assertFalse(oldOperation.isExpired());
      assertFalse(nonOldOperation.isExpired());
      verifyAllFilesPresent(oldOperation);
      verifyAllFilesPresent(nonOldOperation);
      logFilesService.clearLogFiles();
      oldOperation = bulkOperationRepository.findById(oldBulkOperationId).get();
      nonOldOperation = bulkOperationRepository.findById(nonOldBulkOperationId).get();
      assertTrue(oldOperation.isExpired());
      assertFalse(nonOldOperation.isExpired());
      verifyAllFilesRemoved(oldOperation);
      verifyAllFilesPresent(nonOldOperation);
    }
  }

  @Test
  void shouldRemoveCsvAndJsonFilesFromStorageByOperationIdAndFileName() {
    var operationId = UUID.randomUUID();
    var csvPath = String.format(CSV_PATH_TEMPLATE, operationId, "file");
    var jsonPath = String.format(JSON_PATH_TEMPLATE, operationId, "file");

    client.put(IOUtils.toInputStream("csv content", StandardCharsets.UTF_8), csvPath);
    client.put(IOUtils.toInputStream("json content", StandardCharsets.UTF_8), jsonPath);

    logFilesService.deleteFileByOperationIdAndName(operationId, "file.csv");

    assertThrows(S3ClientException.class, () -> client.get(csvPath));
    assertThrows(S3ClientException.class, () -> client.get(jsonPath));
  }

  private void verifyAllFilesRemoved(BulkOperation operation) {
    assertNull(operation.getLinkToCommittedRecordsCsvFile());
    assertNull(operation.getLinkToCommittedRecordsJsonFile());
    assertNull(operation.getLinkToCommittedRecordsMarcFile());
    assertNull(operation.getLinkToCommittedRecordsMarcCsvFile());
    assertNull(operation.getLinkToMatchedRecordsJsonFile());
    assertNull(operation.getLinkToMatchedRecordsCsvFile());
    assertNull(operation.getLinkToTriggeringCsvFile());
    assertNull(operation.getLinkToModifiedRecordsCsvFile());
    assertNull(operation.getLinkToCommittedRecordsErrorsCsvFile());
    assertNull(operation.getLinkToPreviewRecordsJsonFile());
    assertNull(operation.getLinkToModifiedRecordsJsonFile());
    assertNull(operation.getLinkToModifiedRecordsMarcFile());
    assertNull(operation.getLinkToModifiedRecordsMarcCsvFile());
    assertNull(operation.getLinkToMatchedRecordsErrorsCsvFile());
  }

  private void verifyAllFilesPresent(BulkOperation operation) {
    assertNotNull(operation.getLinkToCommittedRecordsCsvFile());
    assertNotNull(operation.getLinkToCommittedRecordsJsonFile());
    assertNotNull(operation.getLinkToCommittedRecordsMarcFile());
    assertNotNull(operation.getLinkToCommittedRecordsMarcCsvFile());
    assertNotNull(operation.getLinkToMatchedRecordsJsonFile());
    assertNotNull(operation.getLinkToMatchedRecordsCsvFile());
    assertNotNull(operation.getLinkToMatchedRecordsMarcFile());
    assertNotNull(operation.getLinkToModifiedRecordsMarcFile());
    assertNotNull(operation.getLinkToModifiedRecordsMarcCsvFile());
    assertNotNull(operation.getLinkToTriggeringCsvFile());
    assertNotNull(operation.getLinkToModifiedRecordsCsvFile());
    assertNotNull(operation.getLinkToCommittedRecordsErrorsCsvFile());
    assertNotNull(operation.getLinkToPreviewRecordsJsonFile());
    assertNotNull(operation.getLinkToModifiedRecordsJsonFile());
    assertNotNull(operation.getLinkToMatchedRecordsErrorsCsvFile());
  }

}
