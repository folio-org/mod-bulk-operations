package org.folio.bulkops.domain.converter;

import org.folio.bulkops.service.HoldingsReferenceHelper;

import static org.apache.commons.lang3.StringUtils.EMPTY;

public class HoldingsTypeConverter extends BaseConverter<String> {

  @Override
  public String convertToObject(String value) {
    return HoldingsReferenceHelper.service().getHoldingsTypeByName(value).getId();
  }

  @Override
  public String convertToString(String object) {
    return HoldingsReferenceHelper.service().getHoldingsTypeById(object).getName();
  }

  @Override
  public String getDefaultObjectValue() {
    return EMPTY;
  }
}
