package org.folio.bulkops.domain.converter;

import com.opencsv.bean.AbstractBeanField;
import com.opencsv.exceptions.CsvConstraintViolationException;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import org.apache.commons.lang3.ObjectUtils;
import org.folio.bulkops.service.HoldingsReferenceService;

import static org.apache.commons.lang3.StringUtils.EMPTY;

public class CallNumberTypeConverter extends AbstractBeanField<String, String> {

  @Override
  protected Object convert(String value) throws CsvDataTypeMismatchException, CsvConstraintViolationException {
    return HoldingsReferenceService.service().getCallNumberTypeIdByName(value);
  }

  @Override
  protected String convertToWrite(Object value) {
    if (ObjectUtils.isNotEmpty(value)) {
      return HoldingsReferenceService.service().getCallNumberTypeNameById(value.toString());
    }
    return EMPTY;
  }
}
