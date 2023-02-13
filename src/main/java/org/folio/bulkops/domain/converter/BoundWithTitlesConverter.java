package org.folio.bulkops.domain.converter;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.folio.bulkops.adapters.Constants.ARRAY_DELIMITER;
import static org.folio.bulkops.adapters.Constants.ITEM_DELIMITER;
import static org.folio.bulkops.domain.format.SpecialCharacterEscaper.escape;

import com.opencsv.bean.AbstractBeanField;
import com.opencsv.exceptions.CsvConstraintViolationException;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import org.apache.commons.lang3.ObjectUtils;
import org.folio.bulkops.domain.bean.Title;

import java.util.List;
import java.util.stream.Collectors;

public class BoundWithTitlesConverter extends AbstractBeanField<String, List<Title>> {
  @Override
  protected Object convert(String value) throws CsvDataTypeMismatchException, CsvConstraintViolationException {
    return null;
  }

  @Override
  protected String convertToWrite(Object value) {
    return ObjectUtils.isEmpty(value) ?
      EMPTY :
      ((List<Title>) value).stream()
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
