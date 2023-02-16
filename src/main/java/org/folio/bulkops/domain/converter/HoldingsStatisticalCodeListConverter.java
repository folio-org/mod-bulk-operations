package org.folio.bulkops.domain.converter;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.folio.bulkops.util.Constants.ARRAY_DELIMITER;

import com.opencsv.bean.AbstractBeanField;
import com.opencsv.exceptions.CsvConstraintViolationException;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import org.apache.commons.lang3.ObjectUtils;
import org.folio.bulkops.domain.format.SpecialCharacterEscaper;
import org.folio.bulkops.service.HoldingsReferenceHelper;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class HoldingsStatisticalCodeListConverter extends AbstractBeanField<String, List<String>> {
  @Override
  protected Object convert(String value) throws CsvDataTypeMismatchException, CsvConstraintViolationException {
    return isEmpty(value) ?
      Collections.emptyList() :
      Arrays.stream(value.split(ARRAY_DELIMITER))
        .map(SpecialCharacterEscaper::restore)
        .map(HoldingsReferenceHelper.service()::getStatisticalCodeIdByName)
        .filter(Objects::nonNull)
        .toList();
  }

  @Override
  protected String convertToWrite(Object value) {
    return ObjectUtils.isEmpty(value) ?
      EMPTY :
      ((List<String>) value).stream()
        .map(HoldingsReferenceHelper.service()::getStatisticalCodeNameById)
        .map(SpecialCharacterEscaper::escape)
        .collect(Collectors.joining(ARRAY_DELIMITER));
  }
}
