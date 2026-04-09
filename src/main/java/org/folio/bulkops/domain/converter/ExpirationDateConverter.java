package org.folio.bulkops.domain.converter;

import static org.folio.bulkops.util.Constants.DATE_WITHOUT_TIME_FORMATTER;
import static org.folio.bulkops.util.Constants.UTC_ZONE;

import java.time.LocalDate;
import java.util.Date;
import java.util.TimeZone;
import lombok.extern.log4j.Log4j2;
import org.folio.bulkops.service.UserReferenceHelper;

@Log4j2
public class ExpirationDateConverter extends DateWithoutTimeConverter {

  @Override
  public Date convertToObject(String value) {
    var zoneId = UTC_ZONE;
    var locale = UserReferenceHelper.service().getTenantLocale();
    if (locale != null && locale.getTimezone() != null) {
      zoneId = TimeZone.getTimeZone(locale.getTimezone()).toZoneId();
    } else {
      log.error("Locale or timezone is null, using default timezone for parsing expiration date");
    }
    return Date.from(
        LocalDate.parse(value, DATE_WITHOUT_TIME_FORMATTER).atStartOfDay(zoneId).toInstant());
  }

  @Override
  public String convertToString(Date object) {
    var locale = UserReferenceHelper.service().getTenantLocale();
    var formatter = DATE_WITHOUT_TIME_FORMAT.get();
    if (locale != null && locale.getTimezone() != null) {
      formatter.setTimeZone(TimeZone.getTimeZone(locale.getTimezone()));
    } else {
      log.error(
          "Locale or timezone is null, using default timezone for formatting expiration date");
    }
    return formatter.format(object);
  }
}
