package org.folio.bulkops.domain.converter;

import com.opencsv.bean.AbstractBeanField;
import com.opencsv.exceptions.CsvConstraintViolationException;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import org.apache.commons.lang3.StringUtils;
import org.folio.bulkops.domain.bean.EffectiveCallNumberComponents;
import org.folio.bulkops.service.ItemReferenceService;

import java.util.ArrayList;
import java.util.List;

import static java.lang.String.join;
import static org.apache.commons.lang3.ObjectUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.folio.bulkops.util.Constants.ARRAY_DELIMITER;
import static org.folio.bulkops.util.Utils.ofEmptyString;

public class EffectiveCallNumberComponentsConverter extends AbstractBeanField<String, EffectiveCallNumberComponents> {

  private static ItemReferenceService service = ItemReferenceService.service();
  @Override
  protected Object convert(String value) throws CsvDataTypeMismatchException, CsvConstraintViolationException {
    return StringUtils.isEmpty(value) ? null : service.getLocationByName(value);
  }

  @Override
  protected String convertToWrite(Object value) {
    if (isEmpty(value)) {
      return EMPTY;
    }
    var components = (EffectiveCallNumberComponents) value;
    List<String> comps = new ArrayList<>();
    ofEmptyString(components.getCallNumber()).ifPresent(comps::add);
    ofEmptyString(components.getCallNumber()).ifPresent(comps::add);
    ofEmptyString(components.getPrefix()).ifPresent(comps::add);
    ofEmptyString(components.getSuffix()).ifPresent(comps::add);
    ofEmptyString(service.getCallNumberTypeNameById(components.getTypeId())).ifPresent(comps::add);
    return join(ARRAY_DELIMITER, comps);
  }
}
