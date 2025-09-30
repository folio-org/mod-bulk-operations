package org.folio.bulkops.domain.converter;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.folio.bulkops.domain.format.SpecialCharacterEscaper.escape;
import static org.folio.bulkops.util.Constants.ITEM_DELIMITER;
import org.folio.bulkops.service.ItemReferenceHelper;

public class ItemStatisticalCodeListConverter extends BaseConverter<List<String>> {

  @Override
  public String convertToString(List<String> object) {
    return object.stream()
      .filter(Objects::nonNull)
      .map(id -> {
        var sc = ItemReferenceHelper.service().getStatisticalCodeById(id);
        var sct = ItemReferenceHelper.service().getStatisticalCodeTypeById(sc.getStatisticalCodeTypeId());
        return String.format("%s: %s - %s", escape(sct.getName()), escape(sc.getCode()), escape(sc.getName()));
      })
      .collect(Collectors.joining(ITEM_DELIMITER));
  }
}
