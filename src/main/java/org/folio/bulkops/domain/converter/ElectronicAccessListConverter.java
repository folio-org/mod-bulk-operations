package org.folio.bulkops.domain.converter;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.ObjectUtils.isEmpty;
import static org.folio.bulkops.util.Constants.ARRAY_DELIMITER;
import static org.folio.bulkops.util.Constants.ITEM_DELIMITER;

import com.opencsv.bean.AbstractBeanField;
import com.opencsv.exceptions.CsvConstraintViolationException;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import org.folio.bulkops.domain.bean.ElectronicAccess;
import org.folio.bulkops.service.ElectronicAccessService;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class ElectronicAccessListConverter extends AbstractBeanField<String, List<ElectronicAccess>> {

  @Override
  protected Object convert(String value) throws CsvDataTypeMismatchException, CsvConstraintViolationException {
    return isEmpty(value) ?
      Collections.emptyList() :
      Arrays.stream(value.split(ARRAY_DELIMITER))
        .map(ElectronicAccessService.service()::restoreElectronicAccessItem)
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
  }

  @Override
  protected String convertToWrite(Object value) {
    return isEmpty(value) ?
      EMPTY :
      ((List<ElectronicAccess>) value).stream()
        .map(ElectronicAccessService.service()::electronicAccessToString)
        .collect(Collectors.joining(ITEM_DELIMITER));
  }
}
