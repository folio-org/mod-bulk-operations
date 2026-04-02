package org.folio.bulkops.domain.converter;

import static java.util.Objects.isNull;

public class BooleanNullConverter extends BooleanConverter {

  @Override
  public String convertToString(Boolean object) {
    return isNull(object) ? "false" : object.toString();
  }
}
