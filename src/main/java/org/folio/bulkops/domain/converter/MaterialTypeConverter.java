package org.folio.bulkops.domain.converter;

import org.folio.bulkops.domain.bean.MaterialType;

public class MaterialTypeConverter extends BaseConverter<MaterialType> {

  @Override
  public String convertToString(MaterialType object) {
    return object.getName();
  }
}
