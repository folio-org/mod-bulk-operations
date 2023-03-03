package org.folio.bulkops.domain.converter;

import com.opencsv.bean.AbstractBeanField;
import com.opencsv.exceptions.CsvConstraintViolationException;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import org.apache.commons.lang3.ObjectUtils;
import org.folio.bulkops.domain.bean.IllPolicy;
import org.folio.bulkops.service.ItemReferenceHelper;

public class ServicePointConverter extends AbstractBeanField<String, IllPolicy> {

  @Override
  protected Object convert(String value) throws CsvDataTypeMismatchException, CsvConstraintViolationException {
    return ItemReferenceHelper.service().getServicePointIdByName(value);
  }

  @Override
  protected String convertToWrite(Object value) {
    if(ObjectUtils.isNotEmpty(value)) {
      return ItemReferenceHelper.service().getServicePointNameById(value.toString());
    }
    return null;
  }
}