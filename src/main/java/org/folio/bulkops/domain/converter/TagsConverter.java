package org.folio.bulkops.domain.converter;

import static org.folio.bulkops.util.Constants.ARRAY_DELIMITER;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.folio.bulkops.domain.bean.Tags;
import org.folio.bulkops.domain.format.SpecialCharacterEscaper;

public class TagsConverter extends BaseConverter<Tags> {

  @Override
  public Tags convertToObject(String value) {
    Tags tags = new Tags();
    List<String> tagList = SpecialCharacterEscaper.restore(Arrays.asList(value.split(ARRAY_DELIMITER)));
    return tags.withTagList(tagList);
  }

  @Override
  public String convertToString(Tags object) {
    return object.getTagList().stream()
      .filter(Objects::nonNull)
      .map(SpecialCharacterEscaper::escape)
      .collect(Collectors.joining(ARRAY_DELIMITER));
  }
}
