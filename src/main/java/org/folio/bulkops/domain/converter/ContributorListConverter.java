package org.folio.bulkops.domain.converter;

import static org.folio.bulkops.util.Constants.ARRAY_DELIMITER;
import static org.folio.bulkops.util.Constants.ARRAY_DELIMITER_SPACED;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.folio.bulkops.domain.bean.ContributorName;

public class ContributorListConverter extends BaseConverter<List<ContributorName>> {

  @Override
  public List<ContributorName> convertToObject(String value) {
    return Arrays.stream(value.split(ARRAY_DELIMITER))
      .map(String::trim)
      .map(new ContributorName()::withName)
      .toList();
  }

  @Override
  public String convertToString(List<ContributorName> object) {
    return object.stream()
      .filter(Objects::nonNull)
      .map(ContributorName::getName)
      .collect(Collectors.joining(ARRAY_DELIMITER_SPACED));
  }
}
