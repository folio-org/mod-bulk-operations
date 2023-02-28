package org.folio.bulkops.adapters;

import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.folio.bulkops.util.Constants.DATE_TIME_PATTERN;
import static org.folio.bulkops.util.Constants.UTC;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.TimeZone;

import lombok.experimental.UtilityClass;

@UtilityClass
public class BulkEditAdapterHelper {
  private static final DateFormat dateFormat;

  static {
    dateFormat = new SimpleDateFormat(DATE_TIME_PATTERN);
    dateFormat.setTimeZone(TimeZone.getTimeZone(UTC));
  }

  public static String dateToString(Date date) {
    return nonNull(date) ? dateFormat.format(date) : EMPTY;
  }

  public static Date dateFromString(String date) {
    if (isNotEmpty(date)) {
      LocalDateTime localDateTime = LocalDateTime.parse(date, DateTimeFormatter.ofPattern(DATE_TIME_PATTERN));
      return Date.from(localDateTime.atZone(ZoneOffset.UTC).toInstant());
    }
    return null;
  }
}
