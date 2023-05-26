package org.folio.bulkops.domain.converter;

public class IntegerConverter extends BaseConverter<Integer> {

  @Override
  public Integer convertToObject(String value) {
    return Integer.parseInt(value);
  }

  @Override
  public String convertToString(Integer object) {
    return object.toString();
  }

  @Override
  public Integer getDefaultObjectValue() {
    return null;
  }
}
