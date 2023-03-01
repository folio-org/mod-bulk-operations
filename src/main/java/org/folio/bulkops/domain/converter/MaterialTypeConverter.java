package org.folio.bulkops.domain.converter;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import org.apache.commons.lang3.ObjectUtils;
import org.folio.bulkops.domain.bean.MaterialType;
import org.folio.bulkops.service.ItemReferenceHelper;

import com.opencsv.bean.AbstractBeanField;
import com.opencsv.exceptions.CsvConstraintViolationException;
import com.opencsv.exceptions.CsvDataTypeMismatchException;

public class MaterialTypeConverter extends AbstractBeanField<String, MaterialType> {
  @Override
  protected Object convert(String value) throws CsvDataTypeMismatchException, CsvConstraintViolationException {
    return isEmpty(value) ? null : ItemReferenceHelper.service().getMaterialTypeByName(value);
  }

  @Override
  protected String convertToWrite(Object value) {
    return ObjectUtils.isEmpty(value) ? EMPTY : ((MaterialType) value).getName();
  }
}
