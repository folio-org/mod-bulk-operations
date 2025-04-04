package org.folio.bulkops.util;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Set;

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

  public static final String DATE_TIME_CONTROL_FIELD = "005";

  public static final char SPACE_CHAR = ' ';

  public static final int ITEM_NOTE_POSITION = 31;
  public static final int HOLDINGS_NOTE_POSITION = 24;
  public static final int INSTANCE_NOTE_POSITION = 23;
  public static final int INSTANCE_ELECTRONIC_ACCESS_POSITION = 24;

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
  public static final String MSG_NO_ADMINISTRATIVE_CHANGE_REQUIRED = "No change in administrative data required";
  public static final String MSG_NO_MARC_CHANGE_REQUIRED = "No change in MARC fields required";
  public static final String MSG_HOLDING_NO_CHANGE_REQUIRED_UNSUPPRESSED_ITEMS_UPDATED = "No change in value for holdings record required, associated unsuppressed item(s) have been updated.";
  public static final String MSG_HOLDING_NO_CHANGE_REQUIRED_SUPPRESSED_ITEMS_UPDATED = "No change in value for holdings record required, associated suppressed item(s) have been updated.";
  public static final String STAFF_ONLY = "(staff only)";
  public static final String GET_ITEMS_BY_HOLDING_ID_QUERY = "holdingsRecordId==%s";
  public static final String GET_HOLDINGS_BY_INSTANCE_ID_QUERY = "instanceId==%s";
  public static final String APPLY_TO_HOLDINGS = "APPLY_TO_HOLDINGS";
  public static final String APPLY_TO_ITEMS = "APPLY_TO_ITEMS";
  public static final char NON_PRINTING_DELIMITER = '\u001f';
  public static final String SPECIAL_ARRAY_DELIMITER = NON_PRINTING_DELIMITER + ARRAY_DELIMITER;
  public static final String SPECIAL_ITEM_DELIMITER = NON_PRINTING_DELIMITER + ITEM_DELIMITER;
  public static final String SPECIAL_ITEM_DELIMITER_REGEX = NON_PRINTING_DELIMITER + ITEM_DELIMITER_PATTERN;
  public static final String ELECTRONIC_ACCESS_HEADINGS = "URL relationship;URI;Link text;Materials specified;URL public note\n";
  public static final String STAFF_ONLY_NOTE_PARAMETER_KEY = "STAFF_ONLY";
  public static final String MSG_ERROR_TEMPLATE_OPTIMISTIC_LOCKING = "The record cannot be saved because it is not the most recent version. Stored version is %s, bulk edit version is %s.";
  public static final String MSG_ERROR_OPTIMISTIC_LOCKING_DEFAULT = "The record cannot be saved because it is not the most recent version.";

  public static final String CSV_MSG_ERROR_TEMPLATE_OPTIMISTIC_LOCKING = "The record cannot be saved because it is not the most recent version. Stored version is %s, bulk edit version is %s.";
  public static final String RECORD_CANNOT_BE_UPDATED_ERROR_TEMPLATE = "%s cannot be updated because the record is associated with %s and %s is not associated with this tenant.";
  public static final String DATA_IMPORT_ERROR_DISCARDED = "An error occurred during the update operation, possibly due to multiple MARC records linked to the same instance. Please review the inventory for potential data inconsistencies.";
  public static final String ITEM_TYPE = "ITEM";
  public static final String HOLDING_TYPE = "HOLDINGS_RECORD";
  public static final Set<String> SPLIT_NOTE_ENTITIES = Set.of(ITEM_TYPE, HOLDING_TYPE);

  public static final String FIELD_999 = "999";
  public static final char INDICATOR_F = 'f';

  public static final String CSV_EXTENSION = "csv";

  public static final String MARC = "MARC";
  public static final String FOLIO = "FOLIO";
  public static final String ERROR_COMMITTING_FILE_NAME_PREFIX = "-Committing-changes-Errors-";
  public static final String ERROR_MATCHING_FILE_NAME_PREFIX = "-Matching-Records-Errors-";
  public static final String ERROR_FILE_NAME_ENDING = "-Errors.csv";
  public static final String ENRICHED_PREFIX = "enriched-";
  public static final String CHANGED_CSV_PATH_TEMPLATE = "%s/%s-Changed-Records-CSV-%s.csv";
  public static final String CHANGED_MARC_PATH_TEMPLATE = "%s/%s-Changed-Records-MARC-%s.mrc";
  public static final String CHANGED_MARC_CSV_PATH_TEMPLATE = "%s/%s-Changed-Records-MARC-CSV-%s.csv";

  public static final byte[] UTF_8_BOM = new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
}
