package org.folio.bulkops.domain.converter;

import org.apache.commons.lang3.StringUtils;

public class StringConverter extends BaseConverter<String> {

  @Override
  public String convertToObject(String value) {
    return StringUtils.isEmpty(value) || StringUtils.isBlank(value) ? null : value;
  }

  @Override
  public String convertToString(String object) {
    return object;
  }

  @Override
  public String getDefaultObjectValue() {
    return null;
  }
}
