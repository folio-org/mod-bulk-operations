package org.folio.bulkops.domain.converter;

import org.folio.bulkops.domain.format.SpecialCharacterEscaper;
import org.folio.bulkops.service.InstanceReferenceHelper;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class InstanceStatisticalCodeListConverter extends BaseConverter<List<String>> {
  @Override
  public List<String> convertToObject(String value) {
    return Arrays.stream(value.split("\\|"))
      .map(v -> v.split("-")[1].trim())
      .map(SpecialCharacterEscaper::restore)
      .map(name -> InstanceReferenceHelper.service().getStatisticalCodeByName(name).getId())
      .filter(Objects::nonNull)
      .toList();
  }

  @Override
  public String convertToString(List<String> object) {
    return object.stream()
      .filter(Objects::nonNull)
      .map(id -> {
        var sc = InstanceReferenceHelper.service().getStatisticalCodeById(id);
        var sct = InstanceReferenceHelper.service().getStatisticalCodeTypeById(sc.getStatisticalCodeTypeId());
        return String.format("%s: %s - %s", (sct.getName()), sc.getCode(), sc.getName());
      })
      .collect(Collectors.joining("|"));
  }
}
