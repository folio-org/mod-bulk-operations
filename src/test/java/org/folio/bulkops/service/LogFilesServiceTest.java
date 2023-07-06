package org.folio.bulkops.service;

import org.folio.bulkops.BaseTest;
import org.folio.bulkops.domain.entity.BulkOperation;
import org.folio.bulkops.repository.BulkOperationRepository;
import org.folio.spring.scope.FolioExecutionContextSetter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

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
        .linkToModifiedRecordsCsvFile("some/path/records.csv")
        .linkToModifiedRecordsJsonFile("some/path/records.csv")
        .linkToPreviewRecordsJsonFile("some/path/records.csv")
        .linkToCommittedRecordsJsonFile("some/path/records.csv")
        .linkToMatchedRecordsCsvFile("some/path/records.csv")
        .linkToMatchedRecordsErrorsCsvFile("some/path/records.csv")
        .linkToCommittedRecordsErrorsCsvFile("some/path/records.csv")
        .linkToCommittedRecordsCsvFile("some/path/records.csv")
        .endTime(back31days)
        .build()).getId();
      nonOldBulkOperationId = bulkOperationRepository.save(BulkOperation.builder()
        .id(UUID.randomUUID())
        .linkToTriggeringCsvFile("some/path/records.csv")
        .linkToMatchedRecordsJsonFile("some/path/records.csv")
        .linkToModifiedRecordsCsvFile("some/path/records.csv")
        .linkToModifiedRecordsJsonFile("some/path/records.csv")
        .linkToPreviewRecordsJsonFile("some/path/records.csv")
        .linkToCommittedRecordsJsonFile("some/path/records.csv")
        .linkToMatchedRecordsCsvFile("some/path/records.csv")
        .linkToMatchedRecordsErrorsCsvFile("some/path/records.csv")
        .linkToCommittedRecordsErrorsCsvFile("some/path/records.csv")
        .linkToCommittedRecordsCsvFile("some/path/records.csv")
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
    try (var ignored =  new FolioExecutionContextSetter(folioExecutionContext)) {
      var oldOperation = bulkOperationRepository.findById(oldBulkOperationId).get();
      var nonOldOperation = bulkOperationRepository.findById(nonOldBulkOperationId).get();
      assertEquals(false, oldOperation.isExpired());
      assertEquals(false, nonOldOperation.isExpired());
      verifyAllFilesPresent(oldOperation);
      verifyAllFilesPresent(nonOldOperation);
      logFilesService.clearLogFiles();
      oldOperation = bulkOperationRepository.findById(oldBulkOperationId).get();
      nonOldOperation = bulkOperationRepository.findById(nonOldBulkOperationId).get();
      assertEquals(true, oldOperation.isExpired());
      assertEquals(false, nonOldOperation.isExpired());
      verifyAllFilesRemoved(oldOperation);
      verifyAllFilesPresent(nonOldOperation);
    }
  }

  private void verifyAllFilesRemoved(BulkOperation operation) {
    assertNull(operation.getLinkToCommittedRecordsCsvFile());
    assertNull(operation.getLinkToCommittedRecordsJsonFile());
    assertNull(operation.getLinkToMatchedRecordsJsonFile());
    assertNull(operation.getLinkToMatchedRecordsCsvFile());
    assertNull(operation.getLinkToTriggeringCsvFile());
    assertNull(operation.getLinkToModifiedRecordsCsvFile());
    assertNull(operation.getLinkToCommittedRecordsErrorsCsvFile());
    assertNull(operation.getLinkToPreviewRecordsJsonFile());
    assertNull(operation.getLinkToModifiedRecordsJsonFile());
    assertNull(operation.getLinkToMatchedRecordsErrorsCsvFile());
  }

  private void verifyAllFilesPresent(BulkOperation operation) {
    assertNotNull(operation.getLinkToCommittedRecordsCsvFile());
    assertNotNull(operation.getLinkToCommittedRecordsJsonFile());
    assertNotNull(operation.getLinkToMatchedRecordsJsonFile());
    assertNotNull(operation.getLinkToMatchedRecordsCsvFile());
    assertNotNull(operation.getLinkToTriggeringCsvFile());
    assertNotNull(operation.getLinkToModifiedRecordsCsvFile());
    assertNotNull(operation.getLinkToCommittedRecordsErrorsCsvFile());
    assertNotNull(operation.getLinkToPreviewRecordsJsonFile());
    assertNotNull(operation.getLinkToModifiedRecordsJsonFile());
    assertNotNull(operation.getLinkToMatchedRecordsErrorsCsvFile());
  }

}
