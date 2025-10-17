package org.folio.bulkops.processor.preview;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.folio.bulkops.domain.bean.Item;
import org.folio.bulkops.domain.dto.Row;
import org.junit.jupiter.api.Test;

class ItemPreviewProcessorTest {
  @Test
  void getProcessedTypeShouldReturnItemClass() {
    ItemPreviewProcessor processor = new ItemPreviewProcessor();
    assertEquals(Item.class, processor.getProcessedType());
  }

  @Test
  void transformToRowShouldReturnRow() {
    ItemPreviewProcessor processor = new ItemPreviewProcessor();
    Item entity = new Item();
    Row row = processor.transformToRow(entity);
    assertNotNull(row);
  }
}

