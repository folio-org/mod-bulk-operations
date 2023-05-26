package org.folio.bulkops.domain.converter;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.folio.bulkops.util.Constants.DATE_WITHOUT_TIME_PATTERN;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;

import org.apache.commons.lang3.ObjectUtils;

public class DateWithoutTimeConverter extends BaseConverter<Date> {

  @Override
  public Date convertToObject(String value)  {
    try {
      LocalDate localDate = LocalDate.parse(value, DateTimeFormatter.ofPattern(DATE_WITHOUT_TIME_PATTERN));
      return Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
    } catch (Exception e) {
      throw new IllegalArgumentException(String.format("Incorrect datetime value: %s", value));
    }
  }

  @Override
  public String convertToString(Date object) {
    var format = new SimpleDateFormat(DATE_WITHOUT_TIME_PATTERN);
    return ObjectUtils.isNotEmpty(object) ? format.format(object) : EMPTY;
  }
}
