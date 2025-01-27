package org.folio.bulkops.domain.converter;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.folio.bulkops.util.Constants.ARRAY_DELIMITER;

import java.util.Arrays;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.folio.bulkops.domain.dto.ErrorType;
import org.folio.bulkops.domain.format.SpecialCharacterEscaper;
import org.folio.bulkops.exception.ConverterException;
import org.folio.bulkops.exception.ReferenceDataNotFoundException;
import org.folio.bulkops.service.UserReferenceHelper;

public class DepartmentsConverter extends BaseConverter<Set<UUID>> {

  @Override
  public Set<UUID> convertToObject(String value) {
    String[] departmentNames = value.split(ARRAY_DELIMITER);
    if (departmentNames.length > 0) {
      return Arrays.stream(departmentNames)
        .filter(StringUtils::isNotEmpty)
        .map(SpecialCharacterEscaper::restore)
        .map(name -> UserReferenceHelper.service().getDepartmentByName(name))
        .map(d -> UUID.fromString(d.getId()))
        .collect(Collectors.toSet());
    }
    return Collections.emptySet();
  }

  @Override
  public String convertToString(Set<UUID> value) {
    try {
      if (ObjectUtils.isNotEmpty(value)) {
        return value.stream()
          .filter(Objects::nonNull)
          .map(id -> UserReferenceHelper.service().getDepartmentById(id.toString()))
          .filter(ObjectUtils::isNotEmpty)
          .map(v -> SpecialCharacterEscaper.escape(v.getName()))
          .collect(Collectors.joining(ARRAY_DELIMITER));
      }
    } catch (ReferenceDataNotFoundException e) {
      throw new ConverterException(this.getField(), value, e.getMessage(), ErrorType.WARNING);
    } catch (Exception e) {
      throw new ConverterException(this.getField(), value, e.getMessage(), ErrorType.ERROR);
    }
    return EMPTY;
  }
}
