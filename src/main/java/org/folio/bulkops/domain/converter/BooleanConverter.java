package org.folio.bulkops.domain.converter;

import com.opencsv.bean.AbstractBeanField;
import com.opencsv.exceptions.CsvConstraintViolationException;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import org.folio.bulkops.exception.UserFormatException;

import static java.util.Objects.isNull;

public class BooleanConverter extends AbstractBeanField<String, Boolean> {

  @Override
  protected Object convert(String value) throws CsvDataTypeMismatchException, CsvConstraintViolationException {
    if (value.matches("true") || value.matches("false")) {
      return Boolean.parseBoolean(value);
    }
    throw new UserFormatException(String.format("Value %s cannot be converted to boolean", value));
  }

  @Override
  protected String convertToWrite(Object value) {
    return (isNull(value) ? Boolean.FALSE : (Boolean) value).toString();
  }
}
