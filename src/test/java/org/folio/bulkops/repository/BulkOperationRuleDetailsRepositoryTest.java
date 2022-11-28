package org.folio.bulkops.repository;

import static org.folio.bulkops.domain.dto.EntityCustomIdentifierType.BARCODE;
import static org.folio.bulkops.domain.dto.EntityType.USER;
import static org.folio.bulkops.domain.dto.OperationStatusType.NEW;
import static org.folio.bulkops.domain.dto.OperationType.UPDATE;
import static org.folio.bulkops.domain.dto.UpdateActionType.CLEAR_FIELD;
import static org.folio.bulkops.domain.dto.UpdateActionType.REPLACE_WITH;
import static org.folio.bulkops.domain.dto.UpdateOptionType.STATUS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.folio.bulkops.BaseTest;
import org.folio.bulkops.domain.entity.BulkOperation;
import org.folio.bulkops.domain.entity.BulkOperationRule;
import org.folio.bulkops.domain.entity.BulkOperationRuleDetails;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.UUID;

class BulkOperationRuleDetailsRepositoryTest extends BaseTest {
  @Autowired
  private BulkOperationRuleDetailsRepository repository;

  @Autowired
  private BulkOperationRuleRepository bulkOperationRuleRepository;

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
    var updated = repository.save(created.withUpdateAction(CLEAR_FIELD));
    assertTrue(created.getId().equals(updated.getId()) && CLEAR_FIELD.equals(updated.getUpdateAction()));
  }

  @Test
  void shouldDeleteEntity() {
    var created = repository.save(createEntity());
    repository.deleteById(created.getId());
    assertTrue(repository.findById(created.getId()).isEmpty());
  }

  private BulkOperationRuleDetails createEntity() {
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

    var bulkOperationRule = bulkOperationRuleRepository.save(BulkOperationRule.builder()
      .bulkOperationId(bulkOperation.getId())
      .userId(UUID.randomUUID())
      .updateOption(STATUS)
      .build());

    return BulkOperationRuleDetails.builder()
      .ruleId(bulkOperationRule.getId())
      .updateAction(REPLACE_WITH)
      .updateValue("new value")
      .build();
  }
}
