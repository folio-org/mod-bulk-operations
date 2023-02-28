package org.folio.bulkops.domain.converter;

import static org.apache.commons.lang3.ObjectUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.EMPTY;

import org.folio.bulkops.domain.bean.LoanType;
import org.folio.bulkops.service.ItemReferenceHelper;

import com.opencsv.bean.AbstractBeanField;
import com.opencsv.exceptions.CsvConstraintViolationException;
import com.opencsv.exceptions.CsvDataTypeMismatchException;

public class LoanTypeConverter extends AbstractBeanField<String, LoanType> {

  @Override
  protected Object convert(String value) throws CsvDataTypeMismatchException, CsvConstraintViolationException {
    return isEmpty(value) ? null : ItemReferenceHelper.service().getLoanTypeByName(value);
  }

  @Override
  protected String convertToWrite(Object value) {
    if (isEmpty(value)) {
      return EMPTY;
    }
    var name = ((LoanType) value).getName();
    return isEmpty(name) ? EMPTY : name;
  }
}
