package org.folio.bulkops.domain.converter;

import com.opencsv.bean.AbstractBeanField;
import com.opencsv.exceptions.CsvConstraintViolationException;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import org.folio.bulkops.service.HoldingsReferenceService;

public class HoldingsLocationConverter extends AbstractBeanField<String, String> {

  @Override
  protected Object convert(String value) throws CsvDataTypeMismatchException, CsvConstraintViolationException {
    return HoldingsReferenceService.service().getLocationIdByName(value);
  }

  @Override
  protected String convertToWrite(Object value) {
    return HoldingsReferenceService.service().getLocationNameById(value.toString());
  }
}
