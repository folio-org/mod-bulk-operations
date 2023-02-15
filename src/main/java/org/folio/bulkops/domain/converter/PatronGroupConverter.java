package org.folio.bulkops.domain.converter;

import com.opencsv.bean.AbstractBeanField;
import com.opencsv.exceptions.CsvConstraintViolationException;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import org.apache.commons.lang3.ObjectUtils;
import org.folio.bulkops.service.UserReferenceService;

import static org.apache.commons.lang3.StringUtils.EMPTY;

public class PatronGroupConverter extends AbstractBeanField<String, String> {

  @Override
  protected Object convert(String value) throws CsvDataTypeMismatchException, CsvConstraintViolationException {
    return ObjectUtils.isNotEmpty(value) ? UserReferenceService.service().getPatronGroupIdByName(value) : EMPTY;
  }

  @Override
  protected String convertToWrite(Object value) {
    if (ObjectUtils.isNotEmpty(value)) {
      return UserReferenceService.service().getPatronGroupNameById(value.toString());
    }
    return EMPTY;
  }
}
