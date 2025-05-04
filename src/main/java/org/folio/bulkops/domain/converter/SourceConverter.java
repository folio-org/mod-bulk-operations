package org.folio.bulkops.domain.converter;

import org.folio.bulkops.service.HoldingsReferenceHelper;

public class SourceConverter extends BaseConverter<String> {

  @Override
  public String convertToString(String object) {
    return HoldingsReferenceHelper.service().getSourceById(object).getName();
  }
}
