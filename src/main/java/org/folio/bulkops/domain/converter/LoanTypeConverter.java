package org.folio.bulkops.domain.converter;

import org.folio.bulkops.domain.bean.LoanType;

public class LoanTypeConverter extends BaseConverter<LoanType> {

  @Override
  public String convertToString(LoanType object) {
    return object.getName();
  }
}
