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
    if ("default".equals(value)) {
      return null;
    }
    try {
      return convertToObject(value);
    } catch (ConverterException e) {
      System.out.println();
      return value;
    }
  }

  @Override
  protected String convertToWrite(Object object) {
    if (ObjectUtils.isEmpty(object)) {
      return "default";
    }
    try {
      return convertToString((T) object);
    } catch (Exception e) {
      throw new ConverterException(this.getField(), object, e.getMessage());
    }
  }

  public abstract T convertToObject(String value);

  public abstract String convertToString(T object);
}
