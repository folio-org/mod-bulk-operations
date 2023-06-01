package org.folio.bulkops.domain.converter;

import static org.folio.bulkops.util.Constants.DATE_TIME_PATTERN;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;

public class DateWithTimeConverter extends BaseConverter<Date> {

  @Override
  public Date convertToObject(String value) {
    LocalDateTime localDateTime = LocalDateTime.parse(value, DateTimeFormatter.ofPattern(DATE_TIME_PATTERN));
    return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
  }

  @Override
  public String convertToString(Date object) {
    return new SimpleDateFormat(DATE_TIME_PATTERN).format(object);
  }
}
