package org.folio.bo.repository;

import static org.folio.bo.domain.dto.EntityCustomIdentifierType.BARCODE;
import static org.folio.bo.domain.dto.EntityType.USER;
import static org.folio.bo.domain.dto.OperationStatusType.NEW;
import static org.folio.bo.domain.dto.OperationType.UPDATE;
import static org.folio.bo.domain.dto.UpdateOptionType.STATUS;
import static org.folio.bo.domain.dto.UpdateOptionType.TEMPORARY_LOAN_TYPE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.folio.bo.BaseTest;
import org.folio.bo.domain.entity.BulkOperation;
import org.folio.bo.domain.entity.BulkOperationRule;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.UUID;

class BulkOperationRuleRepositoryTest extends BaseTest {
  @Autowired
  private BulkOperationRuleRepository repository;

  @Autowired
  private BulkOperationRepository bulkOperationRepository;

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
    var updated = repository.save(created.withUpdateOption(TEMPORARY_LOAN_TYPE));
    assertTrue(created.getId().equals(updated.getId()) && TEMPORARY_LOAN_TYPE.equals(updated.getUpdateOption()));
  }

  @Test
  void shouldDeleteEntity() {
    var created = repository.save(createEntity());
    repository.deleteById(created.getId());
    assertTrue(repository.findById(created.getId()).isEmpty());
  }

  private BulkOperationRule createEntity() {
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

    return BulkOperationRule.builder()
      .bulkOperationId(bulkOperation.getId())
      .userId(UUID.randomUUID())
      .updateOption(STATUS)
      .build();
  }
}
