package org.folio.bulkops.domain.converter;

import static org.folio.bulkops.util.Constants.ITEM_DELIMITER_SPACED;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.folio.bulkops.domain.bean.Series;

public class SeriesListConverter extends BaseConverter<List<Series>> {

  @Override
  public String convertToString(List<Series> object) {
    return object.stream()
      .filter(Objects::nonNull)
      .map(Series::getValue)
      .collect(Collectors.joining(ITEM_DELIMITER_SPACED));
  }
}
