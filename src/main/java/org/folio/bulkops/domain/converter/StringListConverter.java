package org.folio.bulkops.domain.converter;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.folio.bulkops.util.Constants.ARRAY_DELIMITER;

import com.opencsv.bean.AbstractBeanField;
import com.opencsv.exceptions.CsvConstraintViolationException;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import org.apache.commons.lang3.ObjectUtils;
import org.folio.bulkops.domain.format.SpecialCharacterEscaper;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class StringListConverter extends AbstractBeanField<String, List<String>> {

  @Override
  protected Object convert(String value) throws CsvDataTypeMismatchException, CsvConstraintViolationException {
    return isEmpty(value) ?
      Collections.emptyList() :
      SpecialCharacterEscaper.restore(Arrays.asList(value.split(ARRAY_DELIMITER)));
  }

  @Override
  protected String convertToWrite(Object value) {
    return ObjectUtils.isEmpty(value) ? EMPTY : String.join(ARRAY_DELIMITER, SpecialCharacterEscaper.escape((List<String>) value));
  }
}
