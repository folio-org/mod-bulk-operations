package org.folio.bulkops.domain.converter;

import static org.apache.commons.lang3.StringUtils.EMPTY;

import org.apache.commons.lang3.ObjectUtils;
import org.folio.bulkops.service.UserReferenceHelper;

import com.opencsv.bean.AbstractBeanField;
import com.opencsv.exceptions.CsvConstraintViolationException;
import com.opencsv.exceptions.CsvDataTypeMismatchException;

public class PatronGroupConverter extends AbstractBeanField<String, String> {

  @Override
  protected Object convert(String value) throws CsvDataTypeMismatchException, CsvConstraintViolationException {
    try {
      return UserReferenceHelper.service().getPatronGroupIdByName(value);
    } catch (Exception e) {
      throw new CsvConstraintViolationException(String.format("Patron Group was not found: %s", e.getMessage()));
    }
  }

  @Override
  protected String convertToWrite(Object value) {
    if (ObjectUtils.isNotEmpty(value)) {
      return UserReferenceHelper.service().getPatronGroupNameById(value.toString());
    }
    return EMPTY;
  }
}
