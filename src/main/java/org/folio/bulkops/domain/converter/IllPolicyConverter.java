package org.folio.bulkops.domain.converter;

import org.apache.commons.lang3.ObjectUtils;
import org.folio.bulkops.service.HoldingsReferenceHelper;

import static org.apache.commons.lang3.StringUtils.EMPTY;

public class IllPolicyConverter extends BaseConverter<String> {

  @Override
  protected String convertToWrite(Object value) {
    if (ObjectUtils.isNotEmpty(value)) {

    }
    return EMPTY;
  }

  @Override
  public String convertToObject(String value) {
    return HoldingsReferenceHelper.service().getIllPolicyByName(value).getId();
  }

  @Override
  public String convertToString(String object) {
    return HoldingsReferenceHelper.service().getIllPolicyNameById(object).getName();
  }

  @Override
  public String getDefaultObjectValue() {
    return EMPTY;
  }
}
