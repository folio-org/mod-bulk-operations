package org.folio.bulkops.batch.jobs;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;
import org.folio.bulkops.domain.bean.ExtendedItem;
import org.folio.bulkops.domain.bean.ExtendedItemCollection;
import org.folio.bulkops.domain.bean.Item;
import org.junit.jupiter.api.Test;

class BulkEditItemListProcessorTest {

  @Test
  void process_shouldReturnListOfItems() {
    var item = Item.builder().id(UUID.randomUUID().toString()).build();
    var extendedItem = ExtendedItem.builder().tenantId("tenant").entity(item).build();
    var processor = new BulkEditItemListProcessor();
    var result = processor.process(ExtendedItemCollection.builder()
            .extendedItems(List.of(extendedItem)).totalRecords(1).build());

    assertThat(result).hasSize(1);
    assertThat(result.getFirst().getEntity().getId()).isEqualTo(item.getId());
  }
}
