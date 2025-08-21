package org.folio.bulkops.repository;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.util.ClassUtils.isPresent;

import org.folio.bulkops.BaseTest;
import org.folio.spring.scope.FolioExecutionContextSetter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class AllowedItemStatusesRepositoryTest extends BaseTest {
  @Autowired
  private AllowedItemStatusesRepository repository;

  @Test
  void shouldGetAllowedStatusesByCurrentStatus() {
    try (var context =  new FolioExecutionContextSetter(folioExecutionContext)) {
      var allowedItemStatuses = repository.findByStatus("Available");
      assertThat(allowedItemStatuses.isPresent(), is(true));
      assertThat(allowedItemStatuses.get().getAllowedStatuses().size(), is(9));
    }
  }

  @Test
  void shouldNotGetAllowedStatusesByNonExistingStatus() {
    try (var context =  new FolioExecutionContextSetter(folioExecutionContext)) {
      var allowedItemStatuses = repository.findByStatus("WRONG_STATUS");
      assertThat(allowedItemStatuses.isPresent(), is(false));
    }
  }
}
