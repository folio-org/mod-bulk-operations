package org.folio.bulkops.domain.converter;

import static org.folio.bulkops.util.Constants.ARRAY_DELIMITER;

import org.folio.bulkops.service.HoldingsReferenceHelper;

public class IllPolicyConverter extends BaseConverter<String> {

  @Override
  public String convertToString(String object) {
    var idTenantArr = object.split(ARRAY_DELIMITER);
    return HoldingsReferenceHelper.service().getIllPolicyNameById(idTenantArr[0], idTenantArr.length > 1 ? idTenantArr[1] : null).getName();
  }
}
