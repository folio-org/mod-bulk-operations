package org.folio.bulkops.domain.converter;

import static org.apache.commons.lang3.ObjectUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.folio.bulkops.domain.format.SpecialCharacterEscaper.escape;
import static org.folio.bulkops.util.Constants.ARRAY_DELIMITER;

import com.opencsv.bean.AbstractBeanField;
import com.opencsv.exceptions.CsvConstraintViolationException;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import org.folio.bulkops.domain.bean.EffectiveCallNumberComponents;
import org.folio.bulkops.service.ItemReferenceService;

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
    return String.join(ARRAY_DELIMITER, isEmpty(components.getCallNumber()) ? EMPTY : escape(components.getCallNumber()),
      isEmpty(components.getPrefix()) ? EMPTY : escape(components.getPrefix()),
      isEmpty(components.getSuffix()) ? EMPTY : escape(components.getSuffix()), escape(ItemReferenceService.service()
        .getCallNumberTypeNameById(components.getTypeId())));
  }
}
