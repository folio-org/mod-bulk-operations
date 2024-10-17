package org.folio.bulkops.domain.converter;

import org.folio.bulkops.service.HoldingsReferenceHelper;

import static org.folio.bulkops.util.Constants.ARRAY_DELIMITER;

public class HoldingsLocationConverter extends BaseConverter<String> {

  @Override
  public String convertToObject(String value) {
    return HoldingsReferenceHelper.service().getLocationByName(value).getId();
  }

  @Override
  public String convertToString(String object) {
    var objTenant = object.split(ARRAY_DELIMITER);
    return HoldingsReferenceHelper.service().getLocationById(objTenant[0], objTenant.length > 1 ? objTenant[1] : null).getName();
  }
}
