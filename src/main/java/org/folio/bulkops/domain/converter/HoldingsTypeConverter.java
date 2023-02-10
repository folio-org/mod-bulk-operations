package org.folio.bulkops.domain.converter;

import com.opencsv.bean.AbstractBeanField;
import com.opencsv.exceptions.CsvConstraintViolationException;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import org.folio.bulkops.service.HoldingsReferenceService;

public class HoldingsTypeConverter extends AbstractBeanField<String, String> {

  @Override
  protected Object convert(String value) throws CsvDataTypeMismatchException, CsvConstraintViolationException {
    return HoldingsReferenceService.service().getHoldingsTypeIdByName(value);
  }

  @Override
  protected String convertToWrite(Object value) {
    return HoldingsReferenceService.service().getHoldingsTypeNameById(value.toString());
  }
}
