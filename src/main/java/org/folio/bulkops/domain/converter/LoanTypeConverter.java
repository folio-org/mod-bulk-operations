package org.folio.bulkops.domain.converter;

import org.folio.bulkops.domain.bean.LoanType;
import org.folio.bulkops.service.ItemReferenceHelper;

public class LoanTypeConverter extends BaseConverter<LoanType> {

  @Override
  public String convertToString(LoanType object) {
    return object.getName();
  }

  @Override
  public LoanType convertToObject(String value) {
    return ItemReferenceHelper.service().getLoanTypeByName(value);
  }
}
