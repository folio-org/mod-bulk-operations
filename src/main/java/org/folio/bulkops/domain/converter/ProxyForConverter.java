package org.folio.bulkops.domain.converter;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.folio.bulkops.util.Constants.ARRAY_DELIMITER;

import com.opencsv.bean.AbstractBeanField;
import com.opencsv.exceptions.CsvConstraintViolationException;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;
import org.apache.commons.lang3.ObjectUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class ProxyForConverter extends AbstractBeanField<String, List<String>> {
  @Override
  protected List<String> convert(String value) throws CsvDataTypeMismatchException, CsvConstraintViolationException {
    return isEmpty(value) ? Collections.emptyList() : Arrays.asList(value.split(ARRAY_DELIMITER));
  }

  @Override
  protected String convertToWrite(Object value) throws CsvDataTypeMismatchException, CsvRequiredFieldEmptyException {
    return ObjectUtils.isEmpty(value) ? EMPTY :
      ((List<String>) value).stream()
        .filter(Objects::nonNull)
        .collect(Collectors.joining(ARRAY_DELIMITER));
  }
}
