package org.folio.bulkops.processor;

import lombok.RequiredArgsConstructor;
import org.folio.bulkops.client.ItemClient;
import org.folio.bulkops.domain.dto.Item;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ItemUpdateProcessor implements UpdateProcessor<Item> {
  private final ItemClient itemClient;

  @Override
  public void updateRecord(Item item) {
    itemClient.updateItem(item, item.getId());
  }
}