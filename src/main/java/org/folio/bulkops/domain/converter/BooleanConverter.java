package org.folio.bulkops.domain.converter;

public class BooleanConverter extends BaseConverter<Boolean> {

  @Override
  public Boolean convertToObject(String value) {
      return Boolean.parseBoolean(value);
  }

  @Override
  public String convertToString(Boolean object) {
    return object.toString();
  }

  @Override
  public Boolean getDefaultObjectValue() {
    return null;
  }
}
