package org.folio.bulkops.domain.converter;

import org.folio.bulkops.service.HoldingsReferenceHelper;

public class IllPolicyConverter extends BaseConverter<String> {

  @Override
  public String convertToString(String id) {
    return HoldingsReferenceHelper.service().getIllPolicyNameById(id).getName();
  }
}
