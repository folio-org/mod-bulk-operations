package org.folio.bulkops.domain.converter;

import com.opencsv.bean.AbstractBeanField;
import com.opencsv.exceptions.CsvConstraintViolationException;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import org.folio.bulkops.domain.bean.DamagedStatus;
import org.folio.bulkops.service.ItemReferenceService;

public class DamagedStatusConverter extends AbstractBeanField<String, DamagedStatus> {

  @Override
  protected Object convert(String value) throws CsvDataTypeMismatchException, CsvConstraintViolationException {
    return ItemReferenceService.service().getDamagedStatusIdByName(value);
  }

  @Override
  protected String convertToWrite(Object value) {
    return ItemReferenceService.service().getDamagedStatusNameById(value.toString());
  }
}
