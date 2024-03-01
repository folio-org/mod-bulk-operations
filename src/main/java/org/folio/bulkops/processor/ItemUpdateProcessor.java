package org.folio.bulkops.processor;

import org.folio.bulkops.client.ItemClient;
import org.folio.bulkops.domain.bean.Item;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ItemUpdateProcessor extends AbstractUpdateProcessor<Item> {
  private final ItemClient itemClient;

  @Override
  public void updateRecord(Item item) {
    itemClient.updateItem(item.withHoldingsData(null), item.getId());
  }

  @Override
  public Class<Item> getUpdatedType() {
    return Item.class;
  }
}
