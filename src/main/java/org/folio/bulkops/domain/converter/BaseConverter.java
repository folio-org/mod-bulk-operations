package org.folio.bulkops.domain.converter;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.folio.bulkops.util.Constants.FIELD_ERROR_MESSAGE_PATTERN;

import java.util.Collections;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.folio.bulkops.domain.bean.Tags;
import org.folio.bulkops.exception.ConverterException;

import com.opencsv.bean.AbstractBeanField;
import com.opencsv.exceptions.CsvConstraintViolationException;

public abstract class BaseConverter<T> extends AbstractBeanField<String, T> {

  public static final String FAILED_FIELD_MARKER = "FAILED";
  private volatile boolean failed = false;

  @Override
  protected synchronized Object convert(String value) throws CsvConstraintViolationException {
    if (StringUtils.isEmpty(value) || StringUtils.isBlank(value)) {
      if (this.getField().getType() == Tags.class) {
        return new Tags().withTagList(Collections.emptyList());
      }
      return null;
    }
    try {
      return convertToObject(value);
    } catch (Exception e) {
      throw new CsvConstraintViolationException(format(FIELD_ERROR_MESSAGE_PATTERN, this.getField().getName(), e.getMessage()));
    }
  }

  @Override
  protected synchronized String convertToWrite(Object object) {
    if (ObjectUtils.isEmpty(object)
      || (object.getClass() == Tags.class && ObjectUtils.isEmpty(((Tags) object).getTagList()))) {
      return EMPTY;
    }
    if (failed) {
      failed = false;
      return FAILED_FIELD_MARKER;
    }
    try {
      return convertToString((T) object);
    } catch (Exception e) {
      failed = true;
      throw new ConverterException(this.getField(), object, e.getMessage());
    }
  }

  public abstract T convertToObject(String value);

  public abstract String convertToString(T object);
}
