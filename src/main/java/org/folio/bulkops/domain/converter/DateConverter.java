package org.folio.bulkops.domain.converter;

import com.opencsv.bean.AbstractBeanField;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Objects;

import static java.time.ZoneOffset.UTC;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.folio.bulkops.adapters.Constants.DATE_TIME_PATTERN;

public class DateConverter extends AbstractBeanField<String, Date> {
  @Override
  protected Date convert(String value) {
    if (isNotEmpty(value)) {
      LocalDateTime localDateTime = LocalDateTime.parse(value, DateTimeFormatter.ofPattern(DATE_TIME_PATTERN));
      return Date.from(localDateTime.atZone(UTC).toInstant());
    }
    return null;
  }

  @Override
  protected String convertToWrite(Object value) {
    return (Objects.isNull(value)) ? EMPTY : value.toString();
  }
}
