package org.folio.bulkops.domain.converter;

import org.folio.bulkops.service.HoldingsReferenceHelper;

public class IllPolicyConverter extends BaseConverter<String> {

  @Override
  public String convertToString(String object) {
    return HoldingsReferenceHelper.service().getIllPolicyNameById(object).getName();
  }
}
