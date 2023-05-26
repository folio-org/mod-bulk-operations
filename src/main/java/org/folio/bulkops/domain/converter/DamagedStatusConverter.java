package org.folio.bulkops.domain.converter;

import org.folio.bulkops.service.ItemReferenceHelper;

public class DamagedStatusConverter extends BaseConverter<String> {

  @Override
  public String convertToObject(String value) {
    return ItemReferenceHelper.service().getDamagedStatusByName(value).getId();
  }

  @Override
  public String convertToString(String object) {
    return ItemReferenceHelper.service().getDamagedStatusById(object).getName();
  }
}
