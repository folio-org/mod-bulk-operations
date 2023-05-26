package org.folio.bulkops.domain.converter;

import static java.lang.String.format;

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
      throw new EntityFormatException(format("Illegal status name: %s", value));
    }
  }

  @Override
  public String convertToString(InventoryItemStatus object) {
    return object.getName().getValue();
  }
}
