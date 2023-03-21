package org.folio.bulkops.domain.converter;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.folio.bulkops.util.Constants.ARRAY_DELIMITER;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ObjectUtils;
import org.folio.bulkops.domain.bean.Tags;
import org.folio.bulkops.domain.format.SpecialCharacterEscaper;

import com.opencsv.bean.AbstractBeanField;
import com.opencsv.exceptions.CsvConstraintViolationException;
import com.opencsv.exceptions.CsvDataTypeMismatchException;

public class TagsConverter extends AbstractBeanField<String, Tags> {
  @Override
  protected Object convert(String value) throws CsvDataTypeMismatchException, CsvConstraintViolationException {
    if (isNotEmpty(value)) {
      Tags tags = new Tags();
      List<String> tagList = SpecialCharacterEscaper.restore(Arrays.asList(value.split(ARRAY_DELIMITER)));
      return tags.withTagList(tagList);
    }
    return new Tags().withTagList(Collections.emptyList());
  }

  @Override
  protected String convertToWrite(Object value) {
    if (ObjectUtils.isNotEmpty(value)) {
      var tags = (Tags) value;
      return Objects.isNull(tags.getTagList()) ?
        EMPTY :
        tags.getTagList().stream()
          .filter(Objects::nonNull)
          .map(SpecialCharacterEscaper::escape)
          .collect(Collectors.joining(ARRAY_DELIMITER));
    }
    return EMPTY;
  }
}
