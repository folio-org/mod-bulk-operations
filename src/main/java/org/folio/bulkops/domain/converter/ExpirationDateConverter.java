package org.folio.bulkops.domain.converter;

import java.util.Date;
import java.util.TimeZone;
import lombok.extern.log4j.Log4j2;
import org.folio.bulkops.service.UserReferenceHelper;

@Log4j2
public class ExpirationDateConverter extends DateWithoutTimeConverter {

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
