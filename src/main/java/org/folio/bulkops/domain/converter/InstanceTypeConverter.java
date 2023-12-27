package org.folio.bulkops.domain.converter;

import org.folio.bulkops.service.InstanceReferenceHelper;

public class InstanceTypeConverter extends BaseConverter<String> {

  @Override
  public String convertToObject(String value) {
    return InstanceReferenceHelper.service().getInstanceTypeIdByName(value);
  }

  @Override
  public String convertToString(String object) {
    return InstanceReferenceHelper.service().getInstanceTypeNameById(object);
  }
}
