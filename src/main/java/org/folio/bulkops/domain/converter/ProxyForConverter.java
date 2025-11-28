package org.folio.bulkops.domain.converter;

import static org.folio.bulkops.util.Constants.ARRAY_DELIMITER;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class ProxyForConverter extends BaseConverter<List<String>> {

  @Override
  public List<String> convertToObject(String value) {
    return Arrays.asList(value.split(ARRAY_DELIMITER));
  }

  @Override
  public String convertToString(List<String> object) {
    return object.stream().filter(Objects::nonNull).collect(Collectors.joining(ARRAY_DELIMITER));
  }
}
