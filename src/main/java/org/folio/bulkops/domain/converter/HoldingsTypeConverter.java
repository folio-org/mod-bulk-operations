package org.folio.bulkops.domain.converter;

import static org.folio.bulkops.util.Constants.ARRAY_DELIMITER;

import org.folio.bulkops.service.HoldingsReferenceHelper;

public class HoldingsTypeConverter extends BaseConverter<String> {

  @Override
  public String convertToString(String object) {
    var idTenant = object.split(ARRAY_DELIMITER);
    return HoldingsReferenceHelper.service().getHoldingsTypeById(idTenant[0], idTenant.length > 1 ? idTenant[1] : null).getName();
  }
}
