package org.folio.bulkops.domain.converter;

import static org.folio.bulkops.util.Constants.ITEM_DELIMITER_PATTERN;
import static org.folio.bulkops.util.Constants.ITEM_DELIMITER_SPACED;

import org.folio.bulkops.domain.format.SpecialCharacterEscaper;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class StringListPipedConverter extends BaseConverter<List<String>> {


  @Override
  public List<String> convertToObject(String value) {
    return SpecialCharacterEscaper.restore(Arrays.stream(value.split(ITEM_DELIMITER_PATTERN))
      .map(String::trim)
      .toList());
  }

  @Override
  public String convertToString(List<String> object) {
    return object.stream()
      .filter(Objects::nonNull)
      .map(SpecialCharacterEscaper::escape)
      .collect(Collectors.joining(ITEM_DELIMITER_SPACED));
  }
}
