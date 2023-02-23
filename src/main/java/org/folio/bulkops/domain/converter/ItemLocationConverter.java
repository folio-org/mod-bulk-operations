package org.folio.bulkops.domain.converter;

import static com.github.jknack.handlebars.internal.lang3.ObjectUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.EMPTY;

import com.opencsv.bean.AbstractBeanField;
import com.opencsv.exceptions.CsvConstraintViolationException;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import org.folio.bulkops.domain.bean.ItemLocation;
import org.folio.bulkops.service.ItemReferenceHelper;

public class ItemLocationConverter extends AbstractBeanField<String, ItemLocation> {

  @Override
  protected Object convert(String value) throws CsvDataTypeMismatchException, CsvConstraintViolationException {
    return ItemReferenceHelper.service().getLocationByName(value);
  }

  @Override
  protected String convertToWrite(Object value) {
    if (isEmpty(value)) {
      return EMPTY;
    }
    var name = ((ItemLocation) value).getName();
    return isEmpty(name) ? EMPTY : name;
  }
}
