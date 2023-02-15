package org.folio.bulkops.domain.converter;

import com.opencsv.bean.AbstractBeanField;
import com.opencsv.exceptions.CsvConstraintViolationException;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import org.apache.commons.lang3.ObjectUtils;
import org.folio.bulkops.domain.bean.IllPolicy;
import org.folio.bulkops.service.ItemReferenceService;

public class ServicePointConverter extends AbstractBeanField<String, IllPolicy> {

  @Override
  protected Object convert(String value) throws CsvDataTypeMismatchException, CsvConstraintViolationException {
    return ItemReferenceService.service().getServicePointIdByName(value);
  }

  @Override
  protected String convertToWrite(Object value) {
    if(ObjectUtils.isNotEmpty(value)) {
      return ItemReferenceService.service().getServicePointNameById(value.toString());
    }
    return null;
  }
}
