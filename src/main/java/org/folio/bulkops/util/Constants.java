package org.folio.bulkops.util;

import java.time.ZoneId;

import lombok.experimental.UtilityClass;

@UtilityClass
public class Constants {

  public static final String COMMA_DELIMETER = ",";
  public static final String ARRAY_DELIMITER = ";";
  public static final String ITEM_DELIMITER = "|";
  public static final String NEW_LINE_SEPARATOR = "\n";
  public static final String UTC = "UTC";
  public static final ZoneId UTC_ZONE = ZoneId.of(UTC);
  public static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss.SSSX";
  public static final String DATE_WITHOUT_TIME_PATTERN = "yyyy-MM-dd";
  public static final String ELECTRONIC_RELATIONSHIP_NAME_ID_DELIMITER = ARRAY_DELIMITER;
  public static final String ITEM_DELIMITER_PATTERN = "\\|";
  public static final String KEY_VALUE_DELIMITER = ":";
  public static final String LINE_BREAK = "\n";
  public static final String LINE_BREAK_REPLACEMENT = "\\n";
  public static final String QUERY_PATTERN_NAME = "name==\"%s\"";
  public static final String BULK_EDIT_CONFIGURATIONS_QUERY_TEMPLATE = "module==%s and configName==%s";
  public static final String QUERY_PATTERN_CODE = "code==\"%s\"";
  public static final String QUERY_PATTERN_USERNAME = "username==\"%s\"";
  public static final String QUERY_PATTERN_DESC = "desc==\"%s\"";
  public static final String QUERY_PATTERN_GROUP = "group==\"%s\"";
  public static final String QUERY_PATTERN_REF_ID = "refId==\"%s\"";
  public static final String FIELD_ERROR_MESSAGE_PATTERN = "Field \"%s\" : %s";
}
