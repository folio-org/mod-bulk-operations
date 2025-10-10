package org.folio.bulkops.repository;

import static org.folio.bulkops.domain.dto.IdentifierType.ID;
import static org.folio.bulkops.domain.dto.OperationStatusType.NEW;
import static org.folio.bulkops.domain.dto.OperationType.UPDATE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.UUID;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.folio.bulkops.BaseTest;
import org.folio.bulkops.domain.dto.EntityType;
import org.folio.bulkops.domain.dto.MarcAction;
import org.folio.bulkops.domain.dto.MarcActionDataInner;
import org.folio.bulkops.domain.dto.MarcDataType;
import org.folio.bulkops.domain.dto.MarcParameter;
import org.folio.bulkops.domain.dto.MarcParameterType;
import org.folio.bulkops.domain.dto.MarcSubfieldAction;
import org.folio.bulkops.domain.dto.UpdateActionType;
import org.folio.bulkops.domain.entity.BulkOperation;
import org.folio.bulkops.domain.entity.BulkOperationMarcRule;
import org.folio.spring.scope.FolioExecutionContextSetter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class BulkOperationMarcRuleRepositoryTest extends BaseTest {
  @Autowired
  private BulkOperationMarcRuleRepository repository;

  @Autowired
  private BulkOperationRepository bulkOperationRepository;

  @Test
  @SneakyThrows
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
    var parameter = new MarcParameter().key(MarcParameterType.OVERRIDE_PROTECTED).value("true");
    try (var context =  new FolioExecutionContextSetter(folioExecutionContext)) {
      var created = repository.save(createEntity());
      var updated = repository.save(created.withParameters(Collections.singletonList(parameter)));
      assertTrue(created.getId().equals(updated.getId()) && parameter.equals(updated.getParameters()
              .get(0)));
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

  private BulkOperationMarcRule createEntity() {
    var bulkOperation = bulkOperationRepository.save(BulkOperation.builder()
            .id(UUID.randomUUID())
            .userId(UUID.randomUUID())
            .operationType(UPDATE)
            .entityType(EntityType.INSTANCE)
            .identifierType(ID)
            .status(NEW)
            .dataExportJobId(UUID.randomUUID())
            .totalNumOfRecords(10)
            .processedNumOfRecords(0)
            .executionChunkSize(5)
            .startTime(LocalDateTime.now())
            .build());

    return BulkOperationMarcRule.builder()
      .bulkOperationId(bulkOperation.getId())
      .userId(UUID.randomUUID())
      .tag("500")
      .actions(Collections.singletonList(new MarcAction()
        .name(UpdateActionType.FIND)
        .data(Collections.singletonList(new MarcActionDataInner()
          .key(MarcDataType.VALUE)
          .value("text")))))
      .parameters(Collections.singletonList(new MarcParameter()
        .key(MarcParameterType.OVERRIDE_PROTECTED)
        .value("false")))
      .subfields(Collections.singletonList(new MarcSubfieldAction()
        .subfield(StringUtils.EMPTY)
        .actions(Collections.singletonList(new MarcAction()
            .name(UpdateActionType.ADD_TO_EXISTING)
            .data(Collections.singletonList(new MarcActionDataInner()
              .key(MarcDataType.VALUE)
              .value("text")))))))
      .build();
  }
}
