package org.folio.bulkops.domain.converter;

import static org.folio.bulkops.util.Constants.ITEM_DELIMITER_SPACED;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class StringListPipedConverter extends BaseConverter<List<String>> {

  @Override
  public String convertToString(List<String> object) {
    return object.stream()
        .filter(Objects::nonNull)
        .collect(Collectors.joining(ITEM_DELIMITER_SPACED));
  }
}
