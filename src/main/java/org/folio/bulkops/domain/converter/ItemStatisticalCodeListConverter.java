package org.folio.bulkops.domain.converter;

import com.opencsv.bean.AbstractBeanField;
import com.opencsv.exceptions.CsvConstraintViolationException;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import org.apache.commons.lang3.ObjectUtils;
import org.folio.bulkops.domain.format.SpecialCharacterEscaper;
import org.folio.bulkops.service.ItemReferenceService;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.folio.bulkops.util.Constants.ARRAY_DELIMITER;

public class ItemStatisticalCodeListConverter extends AbstractBeanField<String, List<String>> {

  @Override
  protected Object convert(String value) throws CsvDataTypeMismatchException, CsvConstraintViolationException {
    return isEmpty(value) ?
      Collections.emptyList() :
      Arrays.stream(value.split(ARRAY_DELIMITER))
        .map(SpecialCharacterEscaper::restore)
        .map(ItemReferenceService.service()::getStatisticalCodeIdByCode)
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
  }

  @Override
  protected String convertToWrite(Object value) {
    return ObjectUtils.isEmpty(value) ?
      EMPTY :
      ((List<String>) value).stream()
        .map(ItemReferenceService.service()::getStatisticalCodeById)
        .map(SpecialCharacterEscaper::escape)
        .collect(Collectors.joining(ARRAY_DELIMITER));
  }
}
