package org.folio.bulkops.domain.converter;

import static org.folio.bulkops.util.Constants.ITEM_DELIMITER_SPACED;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.folio.bulkops.domain.bean.AlternativeTitle;

public class AlternativeTitleListConverter extends BaseConverter<List<AlternativeTitle>> {

  @Override
  public String convertToString(List<AlternativeTitle> object) {
    return object.stream()
        .filter(Objects::nonNull)
        .map(AlternativeTitle::getAlternativeTitle)
        .filter(Objects::nonNull)
        .collect(Collectors.joining(ITEM_DELIMITER_SPACED));
  }
}
