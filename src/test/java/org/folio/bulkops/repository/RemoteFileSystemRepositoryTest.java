package org.folio.bulkops.repository;

import org.folio.bulkops.BaseTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class RemoteFileSystemRepositoryTest extends BaseTest{

  @Test
  void shouldRetrieveInitialContentAfterGetAndUpdateAfterPut() {
    repository.put("src/test/resources/repository/initial.txt", "initial.txt");
    var content = repository.get("initial.txt");
    assertEquals("initial content", content.trim());
    var uploaded = repository.put("src/test/resources/repository/updated.txt", "initial.txt");
    assertEquals("initial.txt", uploaded);
    content = repository.get("initial.txt");
    assertEquals("updated content", content.trim());
  }

  @Test
  void shouldNotRetrieveContentIfFileNameNotFound() {
    var content = repository.get("initial_wrong.txt");
    assertNull(content);
  }
}
