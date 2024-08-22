package org.folio.bulkops.util;

import java.text.SimpleDateFormat;
import java.util.Date;

public class DateHelper {

  private DateHelper() {}

  private static final String MARC_DATE_TIME_PATTERN = "yyyyMMddHHmmss.SSS";

  public static String getDateTimeForMarc(Date date) {
    var marcDateFormatter = new SimpleDateFormat(MARC_DATE_TIME_PATTERN);
    var dateAsStr = marcDateFormatter.format(date);
    return dateAsStr.substring(0, dateAsStr.length() - 2);
  }
}
