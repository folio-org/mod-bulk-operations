package org.folio.bulkops.domain.converter;

import static org.folio.bulkops.util.Constants.ITEM_DELIMITER_PATTERN;
import static org.folio.bulkops.util.Constants.ITEM_DELIMITER_SPACED;

import org.apache.commons.lang3.StringUtils;
import org.folio.bulkops.domain.format.SpecialCharacterEscaper;
import org.folio.bulkops.service.InstanceReferenceHelper;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class NatureOfContentTermListConverter extends BaseConverter<List<String>> {


  @Override
  public List<String> convertToObject(String value) {
    return StringUtils.isEmpty(value) ?
      Collections.emptyList() :
      Arrays.stream(value.split(ITEM_DELIMITER_PATTERN))
        .map(String::trim)
        .map(SpecialCharacterEscaper::restore)
        .map(InstanceReferenceHelper.service()::getNatureOfContentTermIdByName)
        .toList();
  }

  @Override
  public String convertToString(List<String> object) {
    return object.stream()
      .filter(Objects::nonNull)
      .map(InstanceReferenceHelper.service()::getNatureOfContentTermNameById)
      .map(SpecialCharacterEscaper::escape)
      .collect(Collectors.joining(ITEM_DELIMITER_SPACED));
  }
}
