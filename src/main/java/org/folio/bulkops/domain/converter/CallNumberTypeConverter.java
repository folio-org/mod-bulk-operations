package org.folio.bulkops.domain.converter;

import org.folio.bulkops.service.HoldingsReferenceHelper;

public class CallNumberTypeConverter extends BaseConverter<String> {

  @Override
  public String convertToObject(String value) {
    return HoldingsReferenceHelper.service().getCallNumberTypeIdByName(value);
  }

  @Override
  public String convertToString(String object) {
    return HoldingsReferenceHelper.service().getCallNumberTypeNameById(object);
  }
}
