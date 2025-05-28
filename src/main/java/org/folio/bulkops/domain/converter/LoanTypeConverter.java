package org.folio.bulkops.domain.converter;

import org.folio.bulkops.domain.bean.LoanType;
import org.folio.bulkops.service.ItemReferenceHelper;

public class LoanTypeConverter extends BaseConverter<LoanType> {

  @Override
  public String convertToString(LoanType object) {
    return ItemReferenceHelper.service().getLoanTypeById(object.getId()).getName();
  }
}
