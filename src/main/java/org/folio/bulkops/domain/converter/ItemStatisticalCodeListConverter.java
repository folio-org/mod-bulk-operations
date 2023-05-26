package org.folio.bulkops.domain.converter;

import org.folio.bulkops.domain.format.SpecialCharacterEscaper;
import org.folio.bulkops.service.ItemReferenceHelper;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.folio.bulkops.util.Constants.ARRAY_DELIMITER;

public class ItemStatisticalCodeListConverter extends BaseConverter<List<String>> {

  @Override
  public List<String> convertToObject(String value) {
    return Arrays.stream(value.split(ARRAY_DELIMITER))
      .map(SpecialCharacterEscaper::restore)
      .map(ItemReferenceHelper.service()::getStatisticalCodeIdByCode)
      .filter(Objects::nonNull)
      .collect(Collectors.toList());
  }

  @Override
  public String convertToString(List<String> object) {
    return object.stream()
      .filter(Objects::nonNull)
      .map(ItemReferenceHelper.service()::getStatisticalCodeById)
      .map(SpecialCharacterEscaper::escape)
      .collect(Collectors.joining(ARRAY_DELIMITER));
  }

  @Override
  public List<String> getDefaultObjectValue() {
    return Collections.emptyList();
  }
}
