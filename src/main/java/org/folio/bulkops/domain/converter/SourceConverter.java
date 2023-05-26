package org.folio.bulkops.domain.converter;

import org.folio.bulkops.service.HoldingsReferenceHelper;

public class SourceConverter extends BaseConverter<String> {

  @Override
  public String convertToObject(String value) {
    return HoldingsReferenceHelper.service().getSourceByName(value).getId();
  }

  @Override
  public String convertToString(String object) {
    return HoldingsReferenceHelper.service().getSourceById(object).getName();
  }

  @Override
  public String getDefaultObjectValue() {
    return null;
  }
}
