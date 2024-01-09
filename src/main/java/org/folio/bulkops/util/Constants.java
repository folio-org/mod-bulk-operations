package org.folio.bulkops.util;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import lombok.experimental.UtilityClass;

@UtilityClass
public class Constants {

  public static final String COMMA_DELIMETER = ",";
  public static final String ARRAY_DELIMITER = ";";
  public static final String ARRAY_DELIMITER_SPACED = "; ";
  public static final String ITEM_DELIMITER = "|";
  public static final String ITEM_DELIMITER_SPACED = " | ";
  public static final String NEW_LINE_SEPARATOR = "\n";
  public static final String UTC = "UTC";
  public static final ZoneId UTC_ZONE = ZoneId.of(UTC);
  public static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss.SSSX";
  public static final DateTimeFormatter DATE_WITH_TIME_FORMATTER = DateTimeFormatter.ofPattern(DATE_TIME_PATTERN);
  public static final String DATE_WITHOUT_TIME_PATTERN = "yyyy-MM-dd";
  public static final DateTimeFormatter DATE_WITHOUT_TIME_FORMATTER = DateTimeFormatter.ofPattern(DATE_WITHOUT_TIME_PATTERN);

  public static final int ITEM_NOTE_POSITION = 31;
  public static final int HOLDINGS_NOTE_POSITION = 19;

  public static final String QUERY_ALL_RECORDS = "cql.allRecords=1";

  public static final String ITEM_DELIMITER_PATTERN = "\\|";
  public static final String KEY_VALUE_DELIMITER = ":";

  public static final String LINE_BREAK = "\n";
  public static final String LINE_BREAK_REPLACEMENT = "\\n";

  public static final String QUERY_PATTERN_NAME = "name==%s";
  public static final String BULK_EDIT_CONFIGURATIONS_QUERY_TEMPLATE = "module==%s and configName==%s";
  public static final String QUERY_PATTERN_CODE = "code==%s";
  public static final String QUERY_PATTERN_USERNAME = "username==%s";
  public static final String QUERY_PATTERN_ADDRESS_TYPE = "addressType==%s";
  public static final String QUERY_PATTERN_GROUP = "group==%s";
  public static final String QUERY_PATTERN_REF_ID = "refId==%s";
  public static final String FIELD_ERROR_MESSAGE_PATTERN = "Field \"%s\" : %s";
  public static final String MSG_NO_CHANGE_REQUIRED = "No change in value required";
  public static final String MSG_HOLDING_NO_CHANGE_REQUIRED_UNSUPPRESSED_ITEMS_UPDATED = "No change in value for holdings record required, associated unsuppressed item(s) have been updated.";
  public static final String MSG_HOLDING_NO_CHANGE_REQUIRED_SUPPRESSED_ITEMS_UPDATED = "No change in value for holdings record required, associated suppressed item(s) have been updated.";
  public static final String STAFF_ONLY = "(staff only)";
  public static final String ADMINISTRATIVE_NOTES = "Administrative Notes";
  public static final String ADMINISTRATIVE_NOTE = "Administrative Note";
}
