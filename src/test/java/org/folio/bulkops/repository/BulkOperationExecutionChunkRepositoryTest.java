package org.folio.bulkops.repository;

import static org.folio.bulkops.domain.bean.StateType.PROCESSED;
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
import org.folio.bulkops.domain.entity.BulkOperationExecution;
import org.folio.bulkops.domain.entity.BulkOperationExecutionChunk;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class BulkOperationExecutionChunkRepositoryTest extends BaseTest {
  @Autowired
  private BulkOperationExecutionChunkRepository repository;

  @Autowired
  private BulkOperationRepository bulkOperationRepository;

  @Autowired
  private BulkOperationExecutionRepository bulkOperationExecutionRepository;

  @Test
  void shouldSaveEntity() {
    var saved = repository.save(createEntity());
    assertThat(saved.getId(), notNullValue());
  }

  @Test
  void shouldFindEntityById() {
    var created = repository.save(createEntity());
    var retrieved = repository.findById(created.getId());
    assertTrue(retrieved.isPresent() && created.getId().equals(retrieved.get().getId()));
  }

  @Test
  void shouldUpdateEntity() {
    var created = repository.save(createEntity());
    var endTime = LocalDateTime.now();
    var updated = repository.save(created.withEndTime(endTime));
    assertTrue(created.getId().equals(updated.getId()) && endTime.isEqual(updated.getEndTime()));
  }

  @Test
  void shouldDeleteEntity() {
    var created = repository.save(createEntity());
    repository.deleteById(created.getId());
    assertTrue(repository.findById(created.getId()).isEmpty());
  }

  private BulkOperationExecutionChunk createEntity() {
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

    var bulkOperationExecution = bulkOperationExecutionRepository.save(BulkOperationExecution.builder()
      .bulkOperationId(bulkOperation.getId())
      .userId(UUID.randomUUID())
      .startTime(LocalDateTime.now())
      .processedRecords(5)
      .status(ACTIVE)
      .build());

    return BulkOperationExecutionChunk.builder()
      .bulkOperationExecutionId(bulkOperationExecution.getId())
      .bulkOperationId(bulkOperation.getId())
      .firstRecordIndex(10)
      .lastRecordIndex(20)
      .startTime(LocalDateTime.now())
      .state(PROCESSED)
      .build();
  }
}
