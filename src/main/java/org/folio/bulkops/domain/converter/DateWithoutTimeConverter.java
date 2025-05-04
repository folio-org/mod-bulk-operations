package org.folio.bulkops.domain.converter;

import static org.folio.bulkops.util.Constants.DATE_WITHOUT_TIME_FORMATTER;
import static org.folio.bulkops.util.Constants.DATE_WITHOUT_TIME_PATTERN;
import static org.folio.bulkops.util.Constants.UTC_ZONE;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.Date;

public class DateWithoutTimeConverter extends BaseConverter<Date> {

  private static final ThreadLocal<SimpleDateFormat> DATE_WITHOUT_TIME_FORMAT =
      ThreadLocal.withInitial(() -> new SimpleDateFormat(DATE_WITHOUT_TIME_PATTERN));

  @Override
  public Date convertToObject(String value)  {
    return Date.from(LocalDate.parse(value, DATE_WITHOUT_TIME_FORMATTER).atStartOfDay(UTC_ZONE).toInstant());
  }

  @Override
  public String convertToString(Date object) {
    return DATE_WITHOUT_TIME_FORMAT.get().format(object);
  }
}
