package org.folio.bulkops.repository;

import static org.folio.bulkops.domain.bean.StatusType.ACTIVE;
import static org.folio.bulkops.domain.dto.EntityType.USER;
import static org.folio.bulkops.domain.dto.IdentifierType.BARCODE;
import static org.folio.bulkops.domain.dto.OperationStatusType.NEW;
import static org.folio.bulkops.domain.dto.OperationType.UPDATE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.UUID;
import org.folio.bulkops.BaseTest;
import org.folio.bulkops.domain.entity.BulkOperation;
import org.folio.bulkops.domain.entity.BulkOperationDataProcessing;
import org.folio.spring.scope.FolioExecutionContextSetter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class BulkOperationDataProcessingRepositoryTest extends BaseTest {
  @Autowired
  private BulkOperationDataProcessingRepository repository;

  @Autowired
  private BulkOperationRepository bulkOperationRepository;

  @Test
  void shouldSaveEntity() {
    try (var context =  new FolioExecutionContextSetter(folioExecutionContext)) {
      var saved = repository.save(createEntity());
      assertThat(saved.getBulkOperationId(), notNullValue());
    }
  }

  @Test
  void shouldFindEntityById() {
    try (var context =  new FolioExecutionContextSetter(folioExecutionContext)) {
      var created = repository.save(createEntity());
      var retrieved = repository.findById(created.getBulkOperationId());
      assertTrue(retrieved.isPresent() && created.getBulkOperationId().equals(retrieved.get().getBulkOperationId()));
    }
  }

  @Test
  void shouldUpdateEntity() {
    try (var context =  new FolioExecutionContextSetter(folioExecutionContext)) {
      var created = repository.save(createEntity());
      var endTime = LocalDateTime.now();
      var updated = repository.save(created.withEndTime(endTime));
      assertTrue(created.getBulkOperationId().equals(updated.getBulkOperationId()) && endTime.isEqual(updated.getEndTime()));
    }
  }

  @Test
  void shouldDeleteEntity() {
    try (var context =  new FolioExecutionContextSetter(folioExecutionContext)) {
      var created = repository.save(createEntity());
      repository.deleteById(created.getBulkOperationId());
      assertTrue(repository.findById(created.getBulkOperationId()).isEmpty());
    }
  }

  private BulkOperationDataProcessing createEntity() {
    var bulkOperation = bulkOperationRepository.save(BulkOperation.builder()
      .id(UUID.randomUUID())
      .userId(UUID.randomUUID())
      .operationType(UPDATE)
      .entityType(USER)
      .identifierType(BARCODE)
      .status(NEW)
      .dataExportJobId(UUID.randomUUID())
      .totalNumOfRecords(10)
      .processedNumOfRecords(0)
      .executionChunkSize(5)
      .startTime(LocalDateTime.now())
      .build());

    return BulkOperationDataProcessing.builder()
      .bulkOperationId(bulkOperation.getId())
      .status(ACTIVE)
      .startTime(LocalDateTime.now())
      .totalNumOfRecords(10)
      .processedNumOfRecords(5)
      .build();
  }
}
