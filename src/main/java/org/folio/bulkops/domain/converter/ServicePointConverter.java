package org.folio.bulkops.domain.converter;

import org.folio.bulkops.service.ItemReferenceHelper;

public class ServicePointConverter extends BaseConverter<String> {


  @Override
  public String convertToObject(String value) {
    return ItemReferenceHelper.service().getServicePointByName(value).getId();
  }

  @Override
  public String convertToString(String object) {
    return ItemReferenceHelper.service().getServicePointById(object).getName();
  }
}
