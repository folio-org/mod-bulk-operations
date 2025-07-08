package org.folio.bulkops.domain.converter;

import org.folio.bulkops.service.HoldingsReferenceHelper;

public class SourceConverter extends BaseConverter<String> {

  @Override
  public String convertToString(String id) {
    return HoldingsReferenceHelper.service().getSourceById(id).getName();
  }
}
