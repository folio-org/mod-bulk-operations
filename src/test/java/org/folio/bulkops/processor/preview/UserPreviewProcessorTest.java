package org.folio.bulkops.processor.preview;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.folio.bulkops.domain.bean.User;
import org.folio.bulkops.domain.dto.Row;
import org.junit.jupiter.api.Test;

class UserPreviewProcessorTest {
  @Test
  void getProcessedTypeShouldReturnUserClass() {
    UserPreviewProcessor processor = new UserPreviewProcessor();
    assertEquals(User.class, processor.getProcessedType());
  }

  @Test
  void transformToRowShouldReturnRow() {
    UserPreviewProcessor processor = new UserPreviewProcessor();
    User entity = new User();
    Row row = processor.transformToRow(entity);
    assertNotNull(row);
  }
}
