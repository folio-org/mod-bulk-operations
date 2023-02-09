package org.folio.bulkops.domain.converter;

import com.opencsv.bean.AbstractBeanField;
import com.opencsv.exceptions.CsvConstraintViolationException;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import org.folio.bulkops.domain.bean.Tags;
import org.folio.bulkops.domain.format.SpecialCharacterEscaper;

import java.util.Arrays;
import java.util.List;

import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.folio.bulkops.adapters.Constants.ARRAY_DELIMITER;

public class TagsConverter extends AbstractBeanField<String, Tags> {
  @Override
  protected Object convert(String value) throws CsvDataTypeMismatchException, CsvConstraintViolationException {
    if (isNotEmpty(value)) {
      Tags tags = new Tags();
      List<String> tagList = SpecialCharacterEscaper.restore(Arrays.asList(value.split(ARRAY_DELIMITER)));
      return tags.withTagList(tagList);
    }
    return null;
  }

  @Override
  protected String convertToWrite(Object value) {
    return nonNull(value) ? String.join(ARRAY_DELIMITER, SpecialCharacterEscaper.escape(((Tags) value).getTagList())) : EMPTY;
  }
}
