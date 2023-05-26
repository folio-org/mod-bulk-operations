package org.folio.bulkops.domain.converter;

import org.folio.bulkops.domain.bean.ContributorName;
import org.folio.bulkops.domain.format.SpecialCharacterEscaper;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.folio.bulkops.util.Constants.ARRAY_DELIMITER;

public class ContributorListConverter extends BaseConverter<List<ContributorName>> {

  @Override
  public List<ContributorName> convertToObject(String value) {
    return Arrays.stream(value.split(ARRAY_DELIMITER))
      .map(SpecialCharacterEscaper::restore)
      .map(new ContributorName()::withName)
      .toList();
  }

  @Override
  public String convertToString(List<ContributorName> object) {
    return object.stream()
      .filter(Objects::nonNull)
      .map(ContributorName::getName)
      .map(SpecialCharacterEscaper::escape)
      .collect(Collectors.joining(ARRAY_DELIMITER));
  }

  @Override
  public List<ContributorName> getDefaultObjectValue() {
    return Collections.emptyList();
  }
}
