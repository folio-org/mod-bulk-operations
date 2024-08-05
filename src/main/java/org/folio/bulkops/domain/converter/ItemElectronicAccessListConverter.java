package org.folio.bulkops.domain.converter;

import org.apache.commons.lang3.ObjectUtils;
import org.folio.bulkops.domain.bean.ElectronicAccess;
import org.folio.bulkops.service.ElectronicAccessHelper;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.folio.bulkops.util.Constants.ELECTRONIC_ACCESS_HEADINGS;
import static org.folio.bulkops.util.Constants.ITEM_DELIMITER;
import static org.folio.bulkops.util.Constants.NEW_LINE_SEPARATOR;

public class ItemElectronicAccessListConverter extends BaseConverter<List<ElectronicAccess>> {
  @Override
  public List<ElectronicAccess> convertToObject(String value) {
    var tokens = value.split(NEW_LINE_SEPARATOR, 2);
    var electronicAccessData = tokens.length == 2 ? tokens[1] : value;
    return Arrays.stream(electronicAccessData.split(ITEM_DELIMITER))
      .map(ElectronicAccessHelper.service()::restoreItemElectronicAccessItem)
      .filter(ObjectUtils::isNotEmpty)
      .toList();
  }

  @Override
  public String convertToString(List<ElectronicAccess> object) {
    return ObjectUtils.isEmpty(object) ?
      EMPTY :
      ELECTRONIC_ACCESS_HEADINGS +
        object.stream()
          .filter(Objects::nonNull)
          .map(ElectronicAccessHelper.service()::itemElectronicAccessToString)
          .collect(Collectors.joining(ITEM_DELIMITER));
  }
}
