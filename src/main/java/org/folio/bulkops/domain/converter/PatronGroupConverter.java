package org.folio.bulkops.domain.converter;

import org.folio.bulkops.service.UserReferenceHelper;

public class PatronGroupConverter extends BaseConverter<String> {

  @Override
  public String convertToObject(String value) {
    return UserReferenceHelper.service().getPatronGroupByName(value).getId();
  }

  @Override
  public String convertToString(String object) {
    return UserReferenceHelper.service().getPatronGroupById(object).getGroup();
  }
}
