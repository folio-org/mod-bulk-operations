package org.folio.bulkops.domain.converter;

import com.opencsv.bean.AbstractBeanField;
import com.opencsv.exceptions.CsvConstraintViolationException;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.folio.bulkops.domain.format.SpecialCharacterEscaper;
import org.folio.bulkops.service.UserReferenceService;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.folio.bulkops.adapters.Constants.ARRAY_DELIMITER;

public class UserDepartmentsConverter extends AbstractBeanField<String, Set<UUID>> {

  @Override
  protected Object convert(String value) throws CsvDataTypeMismatchException, CsvConstraintViolationException {
    String[] departmentNames = value.split(ARRAY_DELIMITER);
    if (departmentNames.length > 0) {
      return Arrays.stream(departmentNames).parallel()
        .filter(StringUtils::isNotEmpty)
        .map(SpecialCharacterEscaper::restore)
        .map(name -> UserReferenceService.service().getDepartmentIdByName(name))
        .map(UUID::fromString)
        .collect(Collectors.toSet());
    }
    return Collections.emptySet();
  }

  @Override
  protected String convertToWrite(Object value) {
    if (ObjectUtils.isNotEmpty(value)) {
      return ((Set<UUID>) value).stream()
        .map(id -> UserReferenceService.service().getDepartmentNameById(id.toString()))
        .map(SpecialCharacterEscaper::escape)
        .collect(Collectors.joining(ARRAY_DELIMITER));
    }
    return EMPTY;
  }
}
