package org.folio.bulkops.domain.converter;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Date;

import static org.folio.bulkops.util.Constants.DATE_TIME_PATTERN;

public class DateTimeConverter extends BaseConverter<Date> {

  @Override
  public Date convertToObject(String value) {
    LocalDateTime localDateTime = LocalDateTime.parse(value, DateTimeFormatter.ofPattern(DATE_TIME_PATTERN));
    return Date.from(localDateTime.atZone(ZoneOffset.systemDefault()).toInstant());
  }

  @Override
  public String convertToString(Date object) {
    return new SimpleDateFormat(DATE_TIME_PATTERN).format(object);
  }

  @Override
  public Date getDefaultObjectValue() {
    return null;
  }
}
