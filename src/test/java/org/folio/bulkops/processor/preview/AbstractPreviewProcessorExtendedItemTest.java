package org.folio.bulkops.processor.preview;

import org.folio.bulkops.domain.bean.InventoryItemStatus;
import org.folio.bulkops.domain.bean.InventoryItemStatus.NameEnum;
import org.folio.bulkops.domain.bean.Item;
import org.folio.bulkops.domain.dto.Row;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AbstractPreviewProcessorExtendedItemTest {

  @Test
  void transformsExtendedItemToRowSuccessfully() {
    Item item = new Item();
    item.setBarcode("1234567890");
    item.setStatus(new InventoryItemStatus().withName(NameEnum.AVAILABLE));
    AbstractPreviewProcessor<Item> processor = new ItemPreviewProcessor();

    Row row = processor.transformToRow(item);

    assertNotNull(row);
    assertTrue(row.getRow().contains("1234567890"));
    assertTrue(row.getRow().contains("Available"));
  }

}

