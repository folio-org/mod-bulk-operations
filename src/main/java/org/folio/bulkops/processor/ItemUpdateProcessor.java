package org.folio.bulkops.processor;

import org.folio.bulkops.client.ItemClient;
import org.folio.bulkops.domain.bean.Item;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ItemUpdateProcessor implements UpdateProcessor<Item> {
  private final ItemClient itemClient;

  @Override
  public void updateRecord(Item item, UUID operationId) {
    itemClient.updateItem(item, item.getId());
  }

  @Override
  public Class<Item> getUpdatedType() {
    return Item.class;
  }
}
