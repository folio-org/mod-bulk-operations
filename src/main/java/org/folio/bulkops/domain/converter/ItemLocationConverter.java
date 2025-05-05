package org.folio.bulkops.domain.converter;

import org.folio.bulkops.domain.bean.ItemLocation;

public class ItemLocationConverter extends BaseConverter<ItemLocation> {

  @Override
  public String convertToString(ItemLocation object) {
    return object.getName();
  }
}
