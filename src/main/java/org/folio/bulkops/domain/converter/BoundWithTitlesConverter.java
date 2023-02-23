package org.folio.bulkops.domain.converter;

import com.opencsv.bean.AbstractBeanField;
import com.opencsv.exceptions.CsvConstraintViolationException;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import org.apache.commons.lang3.ObjectUtils;
import org.folio.bulkops.domain.bean.Title;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.folio.bulkops.domain.format.SpecialCharacterEscaper.escape;
import static org.folio.bulkops.util.Constants.ARRAY_DELIMITER;
import static org.folio.bulkops.util.Constants.ITEM_DELIMITER;

public class BoundWithTitlesConverter extends AbstractBeanField<String, List<Title>> {
  @Override
  protected List<Title> convert(String value) throws CsvDataTypeMismatchException, CsvConstraintViolationException {
    return new ArrayList<>();
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
      escape(title.getBriefHoldingsRecord().getHrid()),
      escape(title.getBriefInstance().getHrid()),
      escape(title.getBriefInstance().getTitle()));
  }
}
