package org.folio.bulkops.domain.converter;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.folio.bulkops.util.Constants.ELECTRONIC_ACCESS_HEADINGS;
import static org.folio.bulkops.util.Constants.SPECIAL_ITEM_DELIMITER;

import org.apache.commons.lang3.ObjectUtils;
import org.folio.bulkops.domain.bean.ElectronicAccess;
import org.folio.bulkops.service.ElectronicAccessHelper;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class ElectronicAccessListInstanceConverter extends ElectronicAccessListConverter {

  @Override
  public String convertToString(List<ElectronicAccess> object) {
    return ObjectUtils.isEmpty(object) ?
      EMPTY :
      ELECTRONIC_ACCESS_HEADINGS +
        object.stream()
          .filter(Objects::nonNull)
          .map(ElectronicAccessHelper.service()::electronicAccessInstanceToString)
          .collect(Collectors.joining(SPECIAL_ITEM_DELIMITER));
  }
}
