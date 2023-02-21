package org.folio.bulkops.domain.converter;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.folio.bulkops.util.Constants.ARRAY_DELIMITER;

import com.opencsv.bean.AbstractBeanField;
import com.opencsv.exceptions.CsvConstraintViolationException;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import org.apache.commons.lang3.ObjectUtils;
import org.folio.bulkops.domain.bean.ContributorName;
import org.folio.bulkops.domain.format.SpecialCharacterEscaper;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class ContributorListConverter extends AbstractBeanField<String, List<ContributorName>> {
  @Override
  protected Object convert(String value) throws CsvDataTypeMismatchException, CsvConstraintViolationException {
    return ObjectUtils.isEmpty(value) ?
      Collections.emptyList() :
      Arrays.stream(value.split(ARRAY_DELIMITER))
        .map(SpecialCharacterEscaper::restore)
        .map(new ContributorName()::withName)
        .collect(Collectors.toList());
  }

  @Override
  protected String convertToWrite(Object value) {
    return ObjectUtils.isEmpty(value) ?
      EMPTY :
      ((List<ContributorName>) value).stream()
        .map(ContributorName::getName)
        .map(SpecialCharacterEscaper::escape)
        .collect(Collectors.joining(ARRAY_DELIMITER));
  }
}
