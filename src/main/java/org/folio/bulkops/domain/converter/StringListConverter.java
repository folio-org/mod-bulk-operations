package org.folio.bulkops.domain.converter;

import org.folio.bulkops.domain.format.SpecialCharacterEscaper;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.folio.bulkops.util.Constants.ARRAY_DELIMITER;

public class StringListConverter extends BaseConverter<List<String>> {


  @Override
  public List<String> convertToObject(String value) {
    return SpecialCharacterEscaper.restore(Arrays.asList(value.split(ARRAY_DELIMITER)));
  }

  @Override
  public String convertToString(List<String> object) {
    return object.stream()
      .filter(Objects::nonNull)
      .map(SpecialCharacterEscaper::escape)
      .collect(Collectors.joining(ARRAY_DELIMITER));
  }

  @Override
  public List<String> getDefaultObjectValue() {
    return null;
  }
}
