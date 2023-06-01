package org.folio.bulkops.domain.converter;

import static java.lang.String.format;

public class BooleanConverter extends BaseConverter<Boolean> {

  @Override
  public Boolean convertToObject(String value)  {
    if (value.matches("true") || value.matches("false")) {
      return Boolean.parseBoolean(value);
    }
    throw new IllegalArgumentException(format("Invalid boolean value: %s", value));
  }

  @Override
  public String convertToString(Boolean object) {
    return object.toString();
  }
}
