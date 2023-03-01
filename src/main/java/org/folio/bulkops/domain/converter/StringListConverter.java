package org.folio.bulkops.domain.converter;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.folio.bulkops.util.Constants.ARRAY_DELIMITER;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ObjectUtils;
import org.folio.bulkops.domain.format.SpecialCharacterEscaper;

import com.opencsv.bean.AbstractBeanField;
import com.opencsv.exceptions.CsvConstraintViolationException;
import com.opencsv.exceptions.CsvDataTypeMismatchException;

public class StringListConverter extends AbstractBeanField<String, List<String>> {

  @Override
  protected Object convert(String value) throws CsvDataTypeMismatchException, CsvConstraintViolationException {
    return ObjectUtils.isEmpty(value) ?
      Collections.emptyList() :
      SpecialCharacterEscaper.restore(Arrays.asList(value.split(ARRAY_DELIMITER)));
  }

  @Override
  protected String convertToWrite(Object value) {
    return ObjectUtils.isEmpty(value) ? EMPTY :
      ((List<String>) value).stream()
        .filter(Objects::nonNull)
        .map(SpecialCharacterEscaper::escape)
        .collect(Collectors.joining(ARRAY_DELIMITER));
  }
}
