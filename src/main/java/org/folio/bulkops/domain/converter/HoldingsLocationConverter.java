package org.folio.bulkops.domain.converter;

import com.opencsv.bean.AbstractBeanField;
import com.opencsv.exceptions.CsvConstraintViolationException;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.folio.bulkops.service.HoldingsReferenceService;

import static org.apache.commons.lang3.StringUtils.EMPTY;

public class HoldingsLocationConverter extends AbstractBeanField<String, String> {

  @Override
  protected Object convert(String value) throws CsvDataTypeMismatchException, CsvConstraintViolationException {
    return StringUtils.isNotEmpty(value) ? HoldingsReferenceService.service().getLocationIdByName(value) : EMPTY;
  }

  @Override
  protected String convertToWrite(Object value) {
    return ObjectUtils.isNotEmpty(value) ? HoldingsReferenceService.service().getLocationNameById(value.toString()) : EMPTY;
  }
}
