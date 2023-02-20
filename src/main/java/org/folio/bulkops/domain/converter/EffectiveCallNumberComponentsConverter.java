package org.folio.bulkops.domain.converter;

import static java.lang.String.join;
import static org.apache.commons.lang3.ObjectUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.SPACE;
import static org.folio.bulkops.util.Constants.ARRAY_DELIMITER;
import static org.folio.bulkops.util.Utils.ofEmptyString;

import com.opencsv.bean.AbstractBeanField;
import com.opencsv.exceptions.CsvConstraintViolationException;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import org.folio.bulkops.domain.bean.EffectiveCallNumberComponents;
import org.folio.bulkops.service.ItemReferenceHelper;

import java.util.ArrayList;
import java.util.List;

public class EffectiveCallNumberComponentsConverter extends AbstractBeanField<String, EffectiveCallNumberComponents> {
  @Override
  protected Object convert(String value) throws CsvDataTypeMismatchException, CsvConstraintViolationException {
    return null;
  }

  @Override
  protected String convertToWrite(Object value) {
    if (isEmpty(value)) {
      return EMPTY;
    }
    var components = (EffectiveCallNumberComponents) value;
    List<String> comps = new ArrayList<>();
    ofEmptyString(components.getCallNumber()).ifPresent(comps::add);
    ofEmptyString(components.getPrefix()).ifPresent(comps::add);
    ofEmptyString(components.getSuffix()).ifPresent(comps::add);
    ofEmptyString(ItemReferenceHelper.service().getCallNumberTypeNameById(components.getTypeId())).ifPresent(comps::add);
    return join(SPACE, comps);
  }
}
