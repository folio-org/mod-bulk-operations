package org.folio.bulkops.repository;

import static org.folio.bulkops.domain.dto.EntityType.USER;
import static org.folio.bulkops.domain.dto.IdentifierType.BARCODE;
import static org.folio.bulkops.domain.dto.OperationStatusType.NEW;
import static org.folio.bulkops.domain.dto.OperationType.UPDATE;
import static org.folio.bulkops.domain.dto.UpdateOptionType.SUPPRESS_FROM_DISCOVERY;
import static org.folio.bulkops.domain.dto.UpdateOptionType.TEMPORARY_LOAN_TYPE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.UUID;
import org.folio.bulkops.BaseTest;
import org.folio.bulkops.domain.dto.UpdateActionType;
import org.folio.bulkops.domain.entity.BulkOperation;
import org.folio.bulkops.domain.entity.BulkOperationRule;
import org.folio.bulkops.domain.entity.BulkOperationRuleDetails;
import org.folio.spring.scope.FolioExecutionContextSetter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class BulkOperationRuleRepositoryTest extends BaseTest {
  @Autowired
  private BulkOperationRuleRepository repository;

  @Autowired
  private BulkOperationRepository bulkOperationRepository;

  @Autowired
  private BulkOperationRuleDetailsRepository ruleDetailsRepository;

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
      var updated = repository.save(created.withUpdateOption(TEMPORARY_LOAN_TYPE));
      assertTrue(created.getId().equals(updated.getId())
              && TEMPORARY_LOAN_TYPE.equals(updated.getUpdateOption()));
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

  @Test
  void shouldFetchRuleDetails() {
    try (var context = new FolioExecutionContextSetter(folioExecutionContext)) {
      var created = repository.save(createEntity());
      var details = BulkOperationRuleDetails.builder()
              .ruleId(created.getId())
              .updateAction(UpdateActionType.FIND_AND_REPLACE)
              .initialValue("abc")
              .updatedValue("def")
              .build();

      ruleDetailsRepository.save(details);

      var fetchedRule = repository.findById(created.getId());

      assertTrue(fetchedRule.isPresent());
      assertThat(fetchedRule.get().getRuleDetails(), hasSize(1));
      var fetchedDetails = fetchedRule.get().getRuleDetails().get(0);
      assertThat(details, equalTo(fetchedDetails));
    }
  }

  private BulkOperationRule createEntity() {
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

    return BulkOperationRule.builder()
      .bulkOperationId(bulkOperation.getId())
      .userId(UUID.randomUUID())
      .updateOption(SUPPRESS_FROM_DISCOVERY)
      .build();
  }
}
