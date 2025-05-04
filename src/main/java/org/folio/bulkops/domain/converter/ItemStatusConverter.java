package org.folio.bulkops.domain.converter;

import org.folio.bulkops.domain.bean.InventoryItemStatus;

public class ItemStatusConverter extends BaseConverter<InventoryItemStatus> {

  @Override
  public String convertToString(InventoryItemStatus object) {
    return object.getName().getValue();
  }
}
