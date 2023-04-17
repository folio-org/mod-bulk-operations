package org.folio.bulkops.repository;

import static org.folio.bulkops.domain.dto.EntityType.USER;
import static org.folio.bulkops.domain.dto.IdentifierType.BARCODE;
import static org.folio.bulkops.domain.dto.OperationStatusType.NEW;
import static org.folio.bulkops.domain.dto.OperationType.UPDATE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.UUID;
import org.folio.bulkops.BaseTest;
import org.folio.bulkops.domain.entity.BulkOperation;
import org.folio.spring.scope.FolioExecutionContextSetter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class BulkOperationRepositoryTest extends BaseTest {
  @Autowired
  private BulkOperationRepository repository;

  @Test
  void shouldSaveEntity() {
    try (var context =  new FolioExecutionContextSetter(folioExecutionContext)) {
      var saved = repository.save(createEntity());
      assertThat(saved.getId(), notNullValue());
      // check if default values were set
      assertThat(saved.getTotalNumOfRecords(), is(0));
      assertThat(saved.getProcessedNumOfRecords(), is(0));
    }
  }

  @Test
  void shouldFindEntityById() {
    try (var context =  new FolioExecutionContextSetter(folioExecutionContext)) {
      var created = repository.save(createEntity());
      var retrieved = repository.findById(created.getId());
      assertTrue(retrieved.isPresent() && created.getId().equals(retrieved.get().getId()));
    }
  }

  @Test
  void shouldUpdateEntity() {
    try (var context =  new FolioExecutionContextSetter(folioExecutionContext)) {
      var created = repository.save(createEntity());
      var endTime = LocalDateTime.now();
      created.setEndTime(endTime);
      var updated = repository.save(created);
      assertTrue(created.getId().equals(updated.getId()) && endTime.isEqual(updated.getEndTime()));
    }
  }

  @Test
  void shouldDeleteEntity() {
    try (var context =  new FolioExecutionContextSetter(folioExecutionContext)) {
      var created = repository.save(createEntity());
      repository.deleteById(created.getId());
      assertTrue(repository.findById(created.getId()).isEmpty());
    }
  }

  private BulkOperation createEntity() {
    return BulkOperation.builder()
      .id(UUID.randomUUID())
      .userId(UUID.randomUUID())
      .operationType(UPDATE)
      .entityType(USER)
      .identifierType(BARCODE)
      .status(NEW)
      .dataExportJobId(UUID.randomUUID())
      .executionChunkSize(5)
      .startTime(LocalDateTime.now())
      .build();
  }
}
