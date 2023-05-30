package org.folio.bulkops.domain.converter;

import org.folio.bulkops.domain.bean.ItemLocation;
import org.folio.bulkops.service.ItemReferenceHelper;

public class ItemLocationConverter extends BaseConverter<ItemLocation> {

  @Override
  public ItemLocation convertToObject(String value) {
    return ItemReferenceHelper.service().getLocationByName(value);
  }

  @Override
  public String convertToString(ItemLocation object) {
    return object.getName();
  }
}
