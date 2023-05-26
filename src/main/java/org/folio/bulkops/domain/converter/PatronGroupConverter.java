package org.folio.bulkops.domain.converter;

import org.folio.bulkops.service.UserReferenceHelper;

public class PatronGroupConverter extends BaseConverter<String> {

  @Override
  public String convertToObject(String value) {
    return UserReferenceHelper.service().getPatronGroupIdByName(value);
  }

  @Override
  public String convertToString(String object) {
    return UserReferenceHelper.service().getPatronGroupNameById(object);
  }
}
