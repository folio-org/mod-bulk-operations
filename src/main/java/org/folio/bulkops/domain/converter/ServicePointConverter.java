package org.folio.bulkops.domain.converter;

import static org.apache.commons.lang3.ObjectUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.EMPTY;

import org.folio.bulkops.domain.bean.IllPolicy;
import org.folio.bulkops.service.ItemReferenceHelper;

import com.opencsv.bean.AbstractBeanField;
import com.opencsv.exceptions.CsvConstraintViolationException;
import com.opencsv.exceptions.CsvDataTypeMismatchException;

public class ServicePointConverter extends AbstractBeanField<String, IllPolicy> {

  @Override
  protected Object convert(String value) throws CsvDataTypeMismatchException, CsvConstraintViolationException {
    return ItemReferenceHelper.service().getServicePointIdByName(value);
  }

  @Override
  protected String convertToWrite(Object value) {
    return isEmpty(value) ? EMPTY : ItemReferenceHelper.service().getServicePointNameById(value.toString());
  }
}
