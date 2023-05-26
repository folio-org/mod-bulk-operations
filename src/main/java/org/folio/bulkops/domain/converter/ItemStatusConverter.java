package org.folio.bulkops.domain.converter;

import org.folio.bulkops.domain.bean.InventoryItemStatus;
import org.folio.bulkops.exception.EntityFormatException;

public class ItemStatusConverter extends BaseConverter<InventoryItemStatus> {
  @Override
  public InventoryItemStatus convertToObject(String value) {
    try {
      return InventoryItemStatus.builder()
        .name(InventoryItemStatus.NameEnum.fromValue(value))
        .build();
    } catch (Exception e) {
      throw new EntityFormatException(String.format("Error - Illegal status name: %s", value));
    }
  }

  @Override
  public String convertToString(InventoryItemStatus object) {
    return object.getName().getValue();
  }

  @Override
  public InventoryItemStatus getDefaultObjectValue() {
    return null;
  }
}
