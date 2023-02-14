package org.folio.bulkops.domain.converter;


import com.opencsv.bean.AbstractBeanField;
import com.opencsv.exceptions.CsvConstraintViolationException;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import org.apache.commons.lang3.ObjectUtils;
import org.folio.bulkops.domain.bean.ItemLocation;
import org.folio.bulkops.service.ItemReferenceService;

import static org.apache.commons.lang3.StringUtils.EMPTY;

public class ItemLocationConverter extends AbstractBeanField<String, ItemLocation> {

  @Override
  protected Object convert(String value) throws CsvDataTypeMismatchException, CsvConstraintViolationException {
    return ItemReferenceService.service().getLocationByName(value);
  }

  @Override
  protected String convertToWrite(Object value) {
    return ObjectUtils.isEmpty(value) ? EMPTY : ((ItemLocation) value).getName();
  }
}
