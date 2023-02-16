package org.folio.bulkops.domain.converter;

import com.opencsv.bean.AbstractBeanField;
import com.opencsv.exceptions.CsvConstraintViolationException;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.folio.bulkops.domain.bean.ElectronicAccess;
import org.folio.bulkops.service.ElectronicAccessHelper;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.ObjectUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.folio.bulkops.util.Constants.ARRAY_DELIMITER;
import static org.folio.bulkops.util.Constants.ITEM_DELIMITER;

public class ElectronicAccessListConverter extends AbstractBeanField<String, List<ElectronicAccess>> {

  @Override
  protected Object convert(String value) throws CsvDataTypeMismatchException, CsvConstraintViolationException {
    return isEmpty(value) ?
      Collections.emptyList() :
      Arrays.stream(value.split(ARRAY_DELIMITER))
        .map(ElectronicAccessHelper.service()::restoreElectronicAccessItem)
        .filter(ObjectUtils::isNotEmpty)
        .collect(Collectors.toList());
  }

  @Override
  protected String convertToWrite(Object value) {
    return isEmpty(value) ?
      EMPTY :
      ((List<ElectronicAccess>) value).stream()
        .map(ElectronicAccessHelper.service()::electronicAccessToString)
        .filter(StringUtils::isNotEmpty)
        .collect(Collectors.joining(ITEM_DELIMITER));
  }
}
