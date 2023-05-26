package org.folio.bulkops.domain.converter;

import com.opencsv.bean.AbstractBeanField;
import com.opencsv.exceptions.CsvConstraintViolationException;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import org.apache.commons.lang3.ObjectUtils;
import org.folio.bulkops.exception.ConverterException;

import static org.apache.commons.lang3.StringUtils.EMPTY;

public abstract class BaseConverter<T> extends AbstractBeanField<String, T> {
  @Override
  protected Object convert(String value) throws CsvDataTypeMismatchException, CsvConstraintViolationException {
    if (EMPTY.equals(value)) {
      return getDefaultObjectValue();
    }
    try {
      return convertToObject(value);
    } catch (ConverterException e) {
      throw new ConverterException(this.getField(), value, e.getMessage());
    }
  }

  @Override
  protected String convertToWrite(Object object) {
    if (ObjectUtils.isEmpty(object)) {
      return EMPTY;
    }
    try {
      return convertToString((T) object);
    } catch (Exception e) {
      throw new ConverterException(this.getField(), object, e.getMessage());
    }
  }

  public abstract T convertToObject(String value);

  public abstract String convertToString(T object);

  public abstract T getDefaultObjectValue();
}
