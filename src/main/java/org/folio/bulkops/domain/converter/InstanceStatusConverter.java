package org.folio.bulkops.domain.converter;

import org.folio.bulkops.service.InstanceReferenceHelper;

public class InstanceStatusConverter extends BaseConverter<String> {

  @Override
  public String convertToString(String object) {
    return InstanceReferenceHelper.service().getInstanceStatusNameById(object);
  }
}
