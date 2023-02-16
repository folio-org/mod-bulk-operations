package org.folio.bulkops.domain.converter;

import com.opencsv.bean.AbstractBeanField;
import com.opencsv.exceptions.CsvConstraintViolationException;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import org.apache.commons.lang3.ObjectUtils;
import org.folio.bulkops.domain.bean.EffectiveCallNumberComponents;
import org.folio.bulkops.domain.format.SpecialCharacterEscaper;
import org.folio.bulkops.service.HoldingsReferenceService;
import org.folio.bulkops.service.ItemReferenceService;

import java.util.ArrayList;
import java.util.List;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.folio.bulkops.util.Constants.ARRAY_DELIMITER;
import static org.folio.bulkops.util.Utils.ofEmptyString;

public class CallNumberTypeConverter extends AbstractBeanField<String, String> {

  @Override
  protected Object convert(String value) throws CsvDataTypeMismatchException, CsvConstraintViolationException {
    return HoldingsReferenceService.service().getCallNumberTypeIdByName(value);
  }

  @Override
  protected String convertToWrite(Object value) {
    if (ObjectUtils.isEmpty((value))) {
      return EMPTY;
    }
    var components = (EffectiveCallNumberComponents) value;
    List<String> entries = new ArrayList<>();
    ofEmptyString(components.getCallNumber()).ifPresent(e -> entries.add(SpecialCharacterEscaper.escape(e)));
    ofEmptyString(components.getPrefix()).ifPresent(e -> entries.add(SpecialCharacterEscaper.escape(e)));
    ofEmptyString(components.getSuffix()).ifPresent(e -> entries.add(SpecialCharacterEscaper.escape(e)));
    entries.add(SpecialCharacterEscaper.escape(ItemReferenceService.service().getCallNumberTypeNameById(components.getTypeId())));
    return String.join(ARRAY_DELIMITER, entries);
  }
}
