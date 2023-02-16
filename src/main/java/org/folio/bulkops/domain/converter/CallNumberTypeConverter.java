package org.folio.bulkops.domain.converter;

import static com.github.jknack.handlebars.internal.lang3.ObjectUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.EMPTY;

import com.opencsv.bean.AbstractBeanField;
import com.opencsv.exceptions.CsvConstraintViolationException;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import org.folio.bulkops.service.HoldingsReferenceHelper;

public class CallNumberTypeConverter extends AbstractBeanField<String, String> {

  @Override
  protected Object convert(String value) throws CsvDataTypeMismatchException, CsvConstraintViolationException {
    return HoldingsReferenceHelper.service().getCallNumberTypeIdByName(value);
  }

  @Override
  protected String convertToWrite(Object value) {
    return isEmpty(value) ? EMPTY : HoldingsReferenceHelper.service().getCallNumberTypeNameById(value.toString());
  }
}
