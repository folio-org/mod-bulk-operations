package org.folio.bulkops.domain.converter;

import static org.folio.bulkops.util.Constants.ARRAY_DELIMITER;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.folio.bulkops.domain.format.SpecialCharacterEscaper;
import org.folio.bulkops.service.HoldingsReferenceHelper;

public class HoldingsStatisticalCodeListConverter extends BaseConverter<List<String>> {

  @Override
  public String convertToString(List<String> object) {
    return object.stream()
      .filter(Objects::nonNull)
      .map(idTenant -> {
        var idTenantArr = idTenant.split(ARRAY_DELIMITER);
        return HoldingsReferenceHelper.service().getStatisticalCodeById(idTenantArr[0], idTenantArr.length > 1 ? idTenantArr[1] : null).getName();
      })
      .map(SpecialCharacterEscaper::escape)
      .collect(Collectors.joining(ARRAY_DELIMITER));
  }
}
