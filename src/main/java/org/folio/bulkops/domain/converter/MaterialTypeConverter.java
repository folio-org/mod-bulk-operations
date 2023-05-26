package org.folio.bulkops.domain.converter;

import org.folio.bulkops.domain.bean.MaterialType;
import org.folio.bulkops.service.ItemReferenceHelper;

public class MaterialTypeConverter extends BaseConverter<MaterialType> {

  @Override
  public MaterialType convertToObject(String value) {
    return ItemReferenceHelper.service().getMaterialTypeByName(value);
  }

  @Override
  public String convertToString(MaterialType object) {
    return object.getName();
  }

  @Override
  public MaterialType getDefaultObjectValue() {
    return null;
  }
}
