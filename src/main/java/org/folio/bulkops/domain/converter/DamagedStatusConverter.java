package org.folio.bulkops.domain.converter;

import static org.apache.commons.lang3.StringUtils.EMPTY;

import com.opencsv.bean.AbstractBeanField;
import com.opencsv.exceptions.CsvConstraintViolationException;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import org.apache.commons.lang3.ObjectUtils;
import org.folio.bulkops.domain.bean.DamagedStatus;
import org.folio.bulkops.service.ItemReferenceHelper;

public class DamagedStatusConverter extends AbstractBeanField<String, DamagedStatus> {

  @Override
  protected Object convert(String value) throws CsvDataTypeMismatchException, CsvConstraintViolationException {
    return ItemReferenceHelper.service().getDamagedStatusIdByName(value);
  }

  @Override
  protected String convertToWrite(Object value) {
    if (ObjectUtils.isNotEmpty(value)) {
      return ItemReferenceHelper.service().getDamagedStatusNameById(value.toString());
    }
    return EMPTY;
  }
}
