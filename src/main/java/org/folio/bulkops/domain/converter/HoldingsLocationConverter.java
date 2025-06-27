package org.folio.bulkops.domain.converter;

import org.folio.bulkops.service.HoldingsReferenceHelper;

import static org.folio.bulkops.util.Constants.ARRAY_DELIMITER;

public class HoldingsLocationConverter extends BaseConverter<String> {

  @Override
  public String convertToString(String object) {
    var idTenantArr = object.split(ARRAY_DELIMITER);
    return HoldingsReferenceHelper.service().getLocationById(idTenantArr[0], idTenantArr.length > 1 ? idTenantArr[1] : null).getName();
  }
}
