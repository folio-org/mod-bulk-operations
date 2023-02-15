package org.folio.bulkops.domain.converter;

import com.opencsv.bean.AbstractBeanField;
import com.opencsv.exceptions.CsvConstraintViolationException;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import org.apache.commons.lang3.ObjectUtils;
import org.folio.bulkops.domain.bean.DamagedStatus;
import org.folio.bulkops.service.ItemReferenceService;

import static org.apache.commons.lang3.StringUtils.EMPTY;

public class DamagedStatusConverter extends AbstractBeanField<String, DamagedStatus> {

  @Override
  protected Object convert(String value) throws CsvDataTypeMismatchException, CsvConstraintViolationException {
    return ItemReferenceService.service().getDamagedStatusIdByName(value);
  }

  @Override
  protected String convertToWrite(Object value) {
    if (ObjectUtils.isNotEmpty(value)) {
      return ItemReferenceService.service().getDamagedStatusNameById(value.toString());
    }
    return EMPTY;
  }
}
