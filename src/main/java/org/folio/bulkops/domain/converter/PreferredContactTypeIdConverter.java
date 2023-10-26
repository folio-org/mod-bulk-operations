package org.folio.bulkops.domain.converter;

import org.folio.bulkops.service.UserReferenceHelper;

public class PreferredContactTypeIdConverter extends BaseConverter<String> {

  @Override
  public String convertToObject(String value) {
    return UserReferenceHelper.service().getPreferredContactTypeById(value).getId();
  }

  @Override
  public String convertToString(String object) {
    return convertToObject(object);
  }
}
