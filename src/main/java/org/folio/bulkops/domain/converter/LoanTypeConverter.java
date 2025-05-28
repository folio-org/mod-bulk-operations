package org.folio.bulkops.domain.converter;

import org.folio.bulkops.service.ItemReferenceHelper;

public class LoanTypeConverter extends BaseConverter<String> {

  @Override
  public String convertToString(String id) {
    return ItemReferenceHelper.service().getLoanTypeById(id).getName();
  }
}
