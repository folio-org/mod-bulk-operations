package org.folio.bulkops.domain.converter;

import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.folio.bulkops.util.Constants.FIELD_ERROR_MESSAGE_PATTERN;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.folio.bulkops.domain.bean.Tags;
import org.folio.bulkops.exception.ConverterException;

import com.opencsv.bean.AbstractBeanField;
import com.opencsv.exceptions.CsvConstraintViolationException;
import org.folio.bulkops.exception.ReferenceDataNotFoundException;

public abstract class BaseConverter<T> extends AbstractBeanField<String, T> {

  public static final String FAILED_FIELD_MARKER = "FAILED";
  private volatile boolean failed = false;

  @Override
  protected synchronized Object convert(String value) throws CsvConstraintViolationException {
    if (StringUtils.isEmpty(value) || StringUtils.isBlank(value)) {
      var type = this.getField().getType();
      if (type == Tags.class) {
        return new Tags().withTagList(emptyList());
      } else if (type == List.class) {
        return emptyList();
      } else if (type == Set.class) {
        return emptySet();
      } else if (type == Map.class) {
        return emptyMap();
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
    } catch (ReferenceDataNotFoundException e) {
      failed = true;
      throw new ConverterException(this.getField(), object, e.getMessage(), e.getErrorType());
    } catch (Exception e) {
      failed = true;
      throw new ConverterException(this.getField(), object, e.getMessage());
    }
  }

  public abstract T convertToObject(String value);

  public abstract String convertToString(T object);
}
