package org.folio.bulkops.util;

import java.text.SimpleDateFormat;
import java.util.Date;

public class DateHelper {

  private DateHelper() {}

  private static final SimpleDateFormat marcDateFormatter = new SimpleDateFormat("yyyyMMddHHmmss.SSS");

  public static String getDateTimeForMarc(Date date) {
    var dateAsStr = marcDateFormatter.format(date);
    return dateAsStr.substring(0, dateAsStr.length() - 2);
  }
}
