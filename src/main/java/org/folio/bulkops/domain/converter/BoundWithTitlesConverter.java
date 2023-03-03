package org.folio.bulkops.domain.converter;

import static org.apache.commons.lang3.ObjectUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.folio.bulkops.domain.format.SpecialCharacterEscaper.escape;
import static org.folio.bulkops.util.Constants.ARRAY_DELIMITER;
import static org.folio.bulkops.util.Constants.ITEM_DELIMITER;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ObjectUtils;
import org.folio.bulkops.domain.bean.Title;

import com.opencsv.bean.AbstractBeanField;
import com.opencsv.exceptions.CsvConstraintViolationException;
import com.opencsv.exceptions.CsvDataTypeMismatchException;

public class BoundWithTitlesConverter extends AbstractBeanField<String, List<Title>> {
  @Override
  protected List<Title> convert(String value) throws CsvDataTypeMismatchException, CsvConstraintViolationException {
    return null;
  }

  @Override
  protected String convertToWrite(Object value) {
    return ObjectUtils.isEmpty(value) ?
      EMPTY :
      ((List<Title>) value).stream()
        .filter(Objects::nonNull)
        .map(this::titleToString)
        .collect(Collectors.joining(ITEM_DELIMITER));
  }

  private String titleToString(Title title) {
    return String.join(ARRAY_DELIMITER,
      escape(isEmpty(title.getBriefHoldingsRecord()) ? EMPTY : title.getBriefHoldingsRecord().getHrid()),
      escape(isEmpty(title.getBriefInstance()) ? EMPTY : title.getBriefInstance().getHrid()),
      escape(isEmpty(title.getBriefInstance()) ? EMPTY : title.getBriefInstance().getTitle()));
  }
}
