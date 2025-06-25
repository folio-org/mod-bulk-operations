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
import org.folio.bulkops.batch.CsvRecordContext;
import org.folio.bulkops.domain.bean.Tags;
import org.folio.bulkops.domain.dto.ErrorType;
import org.folio.bulkops.exception.ConverterException;

import com.opencsv.bean.AbstractBeanField;
import com.opencsv.exceptions.CsvConstraintViolationException;
import org.folio.bulkops.exception.ReferenceDataNotFoundException;
import org.folio.bulkops.service.CsvIdentifierContextHelper;

/**
 * Base class for converters that convert between a string representation and an object of type T.
 * Subclasses should implement the {@link #convertToString(Object)} method to provide
 * the specific conversion logic. Subclasses should also override the {@link #convertToObject(String)} method if they need.
 *
 * @param <T> the type of the object to be converted
 */
public abstract class BaseConverter<T> extends AbstractBeanField<String, T> {

  public static final String FAILED_FIELD_MARKER = "UNKNOWN";

  @Override
  protected Object convert(String value) throws CsvConstraintViolationException {
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
  protected String convertToWrite(Object object) {
    if (ObjectUtils.isEmpty(object)
      || (object.getClass() == Tags.class
        && ObjectUtils.isEmpty(((Tags) object).getTagList()))) {
      return EMPTY;
    }
    try {
      return convertToString((T) object);
    } catch (ReferenceDataNotFoundException e) {
      CsvIdentifierContextHelper.service().saveError(CsvRecordContext.getBulkOperationId(), CsvRecordContext.getIdentifier(),
              new ConverterException(this.getField(), object, e.getMessage(), ErrorType.WARNING));
      return FAILED_FIELD_MARKER;
    } catch (Exception e) {
      CsvIdentifierContextHelper.service().saveError(CsvRecordContext.getBulkOperationId(), CsvRecordContext.getIdentifier(),
              new ConverterException(this.getField(), object, e.getMessage(), ErrorType.ERROR));
      return FAILED_FIELD_MARKER;
    }
  }

  public T convertToObject(String value) {
    throw new UnsupportedOperationException("Converting from string to object is not supported");
  }

  public abstract String convertToString(T object);
}
