package org.folio.bulkops.domain.converter;

import org.apache.commons.lang3.ObjectUtils;

import com.opencsv.bean.AbstractBeanField;
import com.opencsv.exceptions.CsvConstraintViolationException;
import com.opencsv.exceptions.CsvDataTypeMismatchException;

public class BooleanConverter extends AbstractBeanField<String, Boolean> {

  @Override
  protected Object convert(String value) throws CsvDataTypeMismatchException, CsvConstraintViolationException {
    if (value.matches("true") || value.matches("false")) {
      return Boolean.parseBoolean(value);
    }
    return null;
  }

  @Override
  protected String convertToWrite(Object value) {
    if (ObjectUtils.isNotEmpty(value)) {
      return ((Boolean) value).toString();
    }
    return null;
  }
}
