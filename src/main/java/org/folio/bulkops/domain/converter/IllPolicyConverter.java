package org.folio.bulkops.domain.converter;

import static org.apache.commons.lang3.StringUtils.EMPTY;

import org.apache.commons.lang3.ObjectUtils;
import org.folio.bulkops.domain.bean.IllPolicy;
import org.folio.bulkops.service.HoldingsReferenceHelper;

import com.opencsv.bean.AbstractBeanField;
import com.opencsv.exceptions.CsvConstraintViolationException;
import com.opencsv.exceptions.CsvDataTypeMismatchException;

public class IllPolicyConverter extends AbstractBeanField<String, IllPolicy> {
  @Override
  protected Object convert(String value) throws CsvDataTypeMismatchException, CsvConstraintViolationException {
    return HoldingsReferenceHelper.service().getIllPolicyIdByName(value);
  }

  @Override
  protected String convertToWrite(Object value) {
    if (ObjectUtils.isNotEmpty(value)) {
      return HoldingsReferenceHelper.service().getIllPolicyNameById(value.toString());
    }
    return EMPTY;
  }
}
