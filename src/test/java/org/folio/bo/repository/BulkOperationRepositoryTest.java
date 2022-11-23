package org.folio.bo.repository;

import static org.folio.bo.domain.dto.EntityCustomIdentifierType.BARCODE;
import static org.folio.bo.domain.dto.EntityType.USER;
import static org.folio.bo.domain.dto.OperationStatusType.NEW;
import static org.folio.bo.domain.dto.OperationType.UPDATE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.folio.bo.BaseTest;
import org.folio.bo.domain.entity.BulkOperation;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.UUID;

class BulkOperationRepositoryTest extends BaseTest {
  @Autowired
  private BulkOperationRepository repository;

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

  private BulkOperation createEntity() {
    return BulkOperation.builder()
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
      .build();
  }
}
