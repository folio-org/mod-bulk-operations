package org.folio.bulkops.domain.converter;

import static org.folio.bulkops.util.Constants.ITEM_DELIMITER_SPACED;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.folio.bulkops.service.InstanceReferenceHelper;

public class NatureOfContentTermListConverter extends BaseConverter<List<String>> {

  @Override
  public String convertToString(List<String> object) {
    return object.stream()
      .filter(Objects::nonNull)
      .map(InstanceReferenceHelper.service()::getNatureOfContentTermNameById)
      .collect(Collectors.joining(ITEM_DELIMITER_SPACED));
  }
}
