package org.folio.bulkops.domain.converter;

import static org.folio.bulkops.domain.format.SpecialCharacterEscaper.restore;
import static org.folio.bulkops.util.Constants.ITEM_DELIMITER_PATTERN;
import static org.folio.bulkops.util.Constants.ITEM_DELIMITER_SPACED;

import org.apache.commons.lang3.StringUtils;
import org.folio.bulkops.domain.bean.Series;
import org.folio.bulkops.domain.format.SpecialCharacterEscaper;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class SeriesListConverter extends BaseConverter<List<Series>> {


  @Override
  public List<Series> convertToObject(String value) {
    return StringUtils.isEmpty(value) ?
      Collections.emptyList() :
      Arrays.stream(value.split(ITEM_DELIMITER_PATTERN))
        .map(String::trim)
        .map(val -> Series.builder().value(restore(val)).build())
        .toList();
  }

  @Override
  public String convertToString(List<Series> object) {
    return object.stream()
      .filter(Objects::nonNull)
      .map(Series::getValue)
      .map(SpecialCharacterEscaper::escape)
      .collect(Collectors.joining(ITEM_DELIMITER_SPACED));
  }
}
