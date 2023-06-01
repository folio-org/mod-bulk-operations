package org.folio.bulkops.domain.converter;

import static org.folio.bulkops.util.Constants.ITEM_DELIMITER;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ObjectUtils;
import org.folio.bulkops.domain.bean.ElectronicAccess;
import org.folio.bulkops.service.ElectronicAccessHelper;

public class ElectronicAccessListConverter extends BaseConverter<List<ElectronicAccess>> {

  @Override
  public List<ElectronicAccess> convertToObject(String value) {
    return Arrays.stream(value.split("\\|"))
      .map(ElectronicAccessHelper.service()::restoreElectronicAccessItem)
      .filter(ObjectUtils::isNotEmpty)
      .toList();
  }

  @Override
  public String convertToString(List<ElectronicAccess> object) {
    return object.stream()
      .filter(Objects::nonNull)
      .map(ElectronicAccessHelper.service()::electronicAccessToString)
      .collect(Collectors.joining(ITEM_DELIMITER));
  }
}
