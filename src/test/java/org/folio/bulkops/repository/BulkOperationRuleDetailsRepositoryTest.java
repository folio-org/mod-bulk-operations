package org.folio.bulkops.repository;

import static org.folio.bulkops.domain.dto.EntityType.USER;
import static org.folio.bulkops.domain.dto.IdentifierType.BARCODE;
import static org.folio.bulkops.domain.dto.OperationStatusType.NEW;
import static org.folio.bulkops.domain.dto.OperationType.UPDATE;
import static org.folio.bulkops.domain.dto.UpdateActionType.CLEAR_FIELD;
import static org.folio.bulkops.domain.dto.UpdateActionType.REPLACE_WITH;
import static org.folio.bulkops.domain.dto.UpdateOptionType.STATUS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.UUID;
import org.folio.bulkops.BaseTest;
import org.folio.bulkops.domain.entity.BulkOperation;
import org.folio.bulkops.domain.entity.BulkOperationRule;
import org.folio.bulkops.domain.entity.BulkOperationRuleDetails;
import org.folio.spring.scope.FolioExecutionContextSetter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class BulkOperationRuleDetailsRepositoryTest extends BaseTest {
  @Autowired
  private BulkOperationRuleDetailsRepository repository;

  @Autowired
  private BulkOperationRuleRepository bulkOperationRuleRepository;

  @Autowired
  private BulkOperationRepository bulkOperationRepository;

  @Test
  void shouldSaveEntity() {
    try (var context =  new FolioExecutionContextSetter(folioExecutionContext)) {
      var saved = repository.save(createEntity());
      assertThat(saved.getId(), notNullValue());
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
      var updated = repository.save(created.withUpdateAction(CLEAR_FIELD));
      assertTrue(created.getId().equals(updated.getId()) && CLEAR_FIELD.equals(updated.getUpdateAction()));
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

  private BulkOperationRuleDetails createEntity() {
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

    var bulkOperationRule = bulkOperationRuleRepository.save(BulkOperationRule.builder()
      .bulkOperationId(bulkOperation.getId())
      .userId(UUID.randomUUID())
      .updateOption(STATUS)
      .build());

    return BulkOperationRuleDetails.builder()
      .ruleId(bulkOperationRule.getId())
      .updateAction(REPLACE_WITH)
      .updatedValue("new value")
      .build();
  }
}
