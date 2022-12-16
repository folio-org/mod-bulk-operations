package org.folio.bulkops.repository;

import static org.folio.bulkops.domain.dto.ErrorParameterName.IDENTIFIER;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.folio.bulkops.BaseTest;
import org.folio.bulkops.domain.dto.Error;
import org.folio.bulkops.domain.dto.Parameter;
import org.folio.bulkops.domain.entity.BulkOperationError;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collections;
import java.util.UUID;

class BulkOperationErrorRepositoryTest extends BaseTest {
  @Autowired
  private BulkOperationErrorRepository repository;

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
    var newError = new Error()
        .message("New message")
        .type("type")
        .code("code")
        .parameters(Collections.singletonList(new Parameter().key("barcode").value("123")));
    var updated = repository.save(created.withError(newError));
    assertTrue(created.getId().equals(updated.getId()) && newError.equals(updated.getError()));
  }

  @Test
  void shouldDeleteEntity() {
    var created = repository.save(createEntity());
    repository.deleteById(created.getId());
    assertTrue(repository.findById(created.getId()).isEmpty());
  }

  private BulkOperationError createEntity() {
    return BulkOperationError.builder()
        .bulkOperationId(UUID.randomUUID())
        .error(new Error()
          .message("Message")
          .type("type")
          .code("code")
          .parameters(Collections.singletonList(new Parameter().key(IDENTIFIER.getValue()).value("123"))))
      .build();
  }
}
