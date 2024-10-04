package org.folio.bulkops.util;

import static org.folio.bulkops.util.Constants.DATE_TIME_CONTROL_FIELD;

import lombok.experimental.UtilityClass;
import org.marc4j.marc.Record;

import java.text.SimpleDateFormat;
import java.util.Date;

@UtilityClass
public class MarcDateHelper {
  private static final String MARC_DATE_TIME_PATTERN = "yyyyMMddHHmmss.SSS";
  private static final int REMOVE_MILLISECONDS_CHARACTERS_COUNT = 2;

  public static String getDateTimeForMarc(Date date) {
    var marcDateFormatter = new SimpleDateFormat(MARC_DATE_TIME_PATTERN);
    var dateAsStr = marcDateFormatter.format(date);
    return dateAsStr.substring(0, dateAsStr.length() - REMOVE_MILLISECONDS_CHARACTERS_COUNT);
  }

  public static void updateDateTimeControlField(Record marcRecord, Date date) {
    marcRecord.getControlFields().stream()
      .filter(f -> DATE_TIME_CONTROL_FIELD.equals(f.getTag())).findFirst()
      .ifPresent(dateTimeControlField -> dateTimeControlField.setData(MarcDateHelper.getDateTimeForMarc(date)));
  }
}
