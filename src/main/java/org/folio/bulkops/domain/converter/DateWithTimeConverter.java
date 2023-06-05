package org.folio.bulkops.domain.converter;

import static org.folio.bulkops.util.Constants.DATE_TIME_PATTERN;
import static org.folio.bulkops.util.Constants.DATE_WITH_TIME_FORMATTER;
import static org.folio.bulkops.util.Constants.UTC_ZONE;

import java.text.Format;
import java.text.SimpleDateFormat;
import java.time.ZonedDateTime;
import java.util.Date;

public class DateWithTimeConverter extends BaseConverter<Date> {

  private static final Format DATE_WITH_TIME_FORMAT = new SimpleDateFormat(DATE_TIME_PATTERN);

  @Override
  public Date convertToObject(String value) {
    return Date.from(ZonedDateTime.parse(value, DATE_WITH_TIME_FORMATTER).withZoneSameInstant(UTC_ZONE).toInstant());
  }

  @Override
  public String convertToString(Date object) {
    return DATE_WITH_TIME_FORMAT.format(object);
  }
}
