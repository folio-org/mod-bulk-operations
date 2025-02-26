package org.folio.bulkops.domain.converter;

import static org.folio.bulkops.domain.format.SpecialCharacterEscaper.escape;

import org.apache.commons.lang3.NotImplementedException;
import org.folio.bulkops.domain.format.SpecialCharacterEscaper;
import org.folio.bulkops.service.InstanceReferenceHelper;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class InstanceStatisticalCodeListConverter extends BaseConverter<List<String>> {
  @Override
  public List<String> convertToObject(String value) {
    throw new NotImplementedException("Shouldn't be used");
  }

  @Override
  public String convertToString(List<String> object) {
    return object.stream()
      .filter(Objects::nonNull)
      .map(id -> {
        var sc = InstanceReferenceHelper.service().getStatisticalCodeById(id);
        var sct = InstanceReferenceHelper.service().getStatisticalCodeTypeById(sc.getStatisticalCodeTypeId());
        return String.format("%s: %s - %s", escape(sct.getName()), escape(sc.getCode()), escape(sc.getName()));
      })
      .collect(Collectors.joining("|"));
  }
}
