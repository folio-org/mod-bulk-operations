package org.folio.bulkops.domain.converter;

import org.folio.bulkops.service.HoldingsReferenceHelper;

import static org.apache.commons.lang3.StringUtils.EMPTY;

public class HoldingsLocationConverter extends BaseConverter<String> {

  @Override
  public String convertToObject(String value) {
    return HoldingsReferenceHelper.service().getLocationByName(value).getId();
  }

  @Override
  public String convertToString(String object) {
    return HoldingsReferenceHelper.service().getLocationById(object).getName();
  }

  @Override
  public String getDefaultObjectValue() {
    return EMPTY;
  }
}
