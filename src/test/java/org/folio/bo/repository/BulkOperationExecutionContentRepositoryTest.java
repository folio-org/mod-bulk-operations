package org.folio.bo.repository;

import static org.folio.bo.domain.dto.EntityCustomIdentifierType.BARCODE;
import static org.folio.bo.domain.dto.EntityType.USER;
import static org.folio.bo.domain.dto.OperationStatusType.NEW;
import static org.folio.bo.domain.dto.OperationType.UPDATE;
import static org.folio.bo.domain.dto.StateType.FAILED;
import static org.folio.bo.domain.dto.StateType.PROCESSED;
import static org.folio.bo.domain.dto.StatusType.ACTIVE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.folio.bo.BaseTest;
import org.folio.bo.domain.entity.BulkOperation;
import org.folio.bo.domain.entity.BulkOperationExecution;
import org.folio.bo.domain.entity.BulkOperationExecutionChunk;
import org.folio.bo.domain.entity.BulkOperationExecutionContent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.UUID;

class BulkOperationExecutionContentRepositoryTest extends BaseTest {
  @Autowired
  private BulkOperationExecutionContentRepository repository;

  @Autowired
  private BulkOperationRepository bulkOperationRepository;

  @Autowired
  private BulkOperationExecutionRepository bulkOperationExecutionRepository;

  @Autowired
  private BulkOperationExecutionChunkRepository bulkOperationExecutionChunkRepository;

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
    var updated = repository.save(created.withState(FAILED));
    assertTrue(created.getId().equals(updated.getId()) && FAILED.equals(updated.getState()));
  }

  @Test
  void shouldDeleteEntity() {
    var created = repository.save(createEntity());
    repository.deleteById(created.getId());
    assertTrue(repository.findById(created.getId()).isEmpty());
  }

  private BulkOperationExecutionContent createEntity() {
    var bulkOperation = bulkOperationRepository.save(BulkOperation.builder()
      .userId(UUID.randomUUID())
      .operationType(UPDATE)
      .entityType(USER)
      .entityCustomIdentifierType(BARCODE)
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

    var bulkOperationExecutionChunk = bulkOperationExecutionChunkRepository.save(BulkOperationExecutionChunk.builder()
      .bulkOperationExecutionId(bulkOperationExecution.getId())
      .bulkOperationId(bulkOperation.getId())
      .firstRecordIndex(10)
      .lastRecordIndex(20)
      .startTime(LocalDateTime.now())
      .state(PROCESSED)
      .build());

    return BulkOperationExecutionContent.builder()
      .customIdentifier("identifier")
      .bulkOperationExecutionChunkId(bulkOperationExecutionChunk.getId())
      .bulkOperationId(bulkOperation.getId())
      .state(PROCESSED)
      .build();
  }
}
