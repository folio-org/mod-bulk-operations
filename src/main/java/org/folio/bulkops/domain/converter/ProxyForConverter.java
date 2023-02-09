package org.folio.bulkops.domain.converter;

import com.opencsv.bean.AbstractBeanField;
import com.opencsv.exceptions.CsvConstraintViolationException;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;

import java.util.Arrays;
import java.util.Collections;

import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.folio.bulkops.adapters.Constants.ARRAY_DELIMITER;

public class ProxyForConverter extends AbstractBeanField<String, String> {
  @Override
  protected Object convert(String value) throws CsvDataTypeMismatchException, CsvConstraintViolationException {
    return isEmpty(value) ? Collections.emptyList() : Arrays.asList(value.split(ARRAY_DELIMITER));
  }

  @Override
  protected String convertToWrite(Object value) throws CsvDataTypeMismatchException, CsvRequiredFieldEmptyException {
    return super.convertToWrite(value);
  }
}
