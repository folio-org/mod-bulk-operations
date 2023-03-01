package org.folio.bulkops.domain.converter;

import static org.apache.commons.lang3.StringUtils.EMPTY;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import com.opencsv.bean.AbstractBeanField;
import com.opencsv.exceptions.CsvConstraintViolationException;
import com.opencsv.exceptions.CsvDataTypeMismatchException;

public class StringConverter extends AbstractBeanField<String, String> {

  @Override
  protected String convert(String value) throws CsvDataTypeMismatchException, CsvConstraintViolationException {
    return StringUtils.isEmpty(value) || StringUtils.isBlank(value) ? null : value;
  }

  @Override
  protected String convertToWrite(Object value) {
    return ObjectUtils.isEmpty(value) ? EMPTY : value.toString();
  }
}
