package org.folio.bulkops.domain.converter;

import com.opencsv.bean.AbstractBeanField;
import com.opencsv.exceptions.CsvConstraintViolationException;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import org.folio.bulkops.service.UserReferenceService;

public class PatronGroupConverter extends AbstractBeanField<String, String> {

  @Override
  protected Object convert(String value) throws CsvDataTypeMismatchException, CsvConstraintViolationException {
    return UserReferenceService.service().getPatronGroupIdByName(value);
  }

  @Override
  protected String convertToWrite(Object value) {
    return UserReferenceService.service().getPatronGroupNameById(value.toString());
  }
}
