package org.folio.bulkops.util;

import lombok.experimental.UtilityClass;

import java.time.ZoneId;

@UtilityClass
public class Constants {

  public static final String COMMA_DELIMETER = ",";
  public static final String ARRAY_DELIMITER = ";";
  public static final String ITEM_DELIMITER = "|";
  public static final String NEW_LINE_SEPARATOR = "\n";
  public static final String UTC = "UTC";
  public static final ZoneId UTC_ZONE = ZoneId.of(UTC);
  public static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss.SSSX";

  public static final String ELECTRONIC_RELATIONSHIP_NAME_ID_DELIMITER = ARRAY_DELIMITER;
  public static final String ITEM_DELIMITER_PATTERN = "\\|";
  public static final String KEY_VALUE_DELIMITER = ":";


  public static final String LINE_BREAK = "\n";
  public static final String LINE_BREAK_REPLACEMENT = "\\n";
}
