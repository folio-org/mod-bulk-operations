package org.folio.bulkops.util;

import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import lombok.experimental.UtilityClass;

@UtilityClass
public class Constants {

  public static final String HYPHEN = "-";
  public static final String SLASH = "/";
  public static final String COMMA_DELIMETER = ",";
  public static final String ARRAY_DELIMITER = ";";
  public static final String ARRAY_DELIMITER_SPACED = "; ";
  public static final String ITEM_DELIMITER = "|";
  public static final String ITEM_DELIMITER_SPACED = " | ";
  public static final String NEW_LINE_SEPARATOR = "\n";
  public static final String UTC = "UTC";
  public static final ZoneId UTC_ZONE = ZoneId.of(UTC);
  public static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss.SSSX";
  public static final DateTimeFormatter DATE_WITH_TIME_FORMATTER =
      DateTimeFormatter.ofPattern(DATE_TIME_PATTERN);
  public static final String DATE_WITHOUT_TIME_PATTERN = "yyyy-MM-dd";
  public static final DateTimeFormatter DATE_WITHOUT_TIME_FORMATTER =
      DateTimeFormatter.ofPattern(DATE_WITHOUT_TIME_PATTERN);

  public static final String DATE_TIME_CONTROL_FIELD = "005";

  public static final String LEADER_TAG = "LDR";

  public static final char SPACE_CHAR = ' ';

  public static final int ITEM_NOTE_POSITION = 31;
  public static final int HOLDINGS_NOTE_POSITION = 24;
  public static final int INSTANCE_NOTE_POSITION = 25;
  public static final int INSTANCE_ELECTRONIC_ACCESS_POSITION = 26;
  public static final int INSTANCE_SUBJECT_POSITION = 27;
  public static final int INSTANCE_CLASSIFICATION_POSITION = 28;
  public static final int INSTANCE_PUBLICATION_POSITION = 16;

  public static final String ITEM_DELIMITER_PATTERN = "\\|";
  public static final String KEY_VALUE_DELIMITER = ":";

  public static final String LINE_BREAK = "\n";
  public static final String LINE_BREAK_REPLACEMENT = "\\n";

  public static final String QUERY_PATTERN_NAME = "name==%s";
  public static final String QUERY_PATTERN_HRID = "hrid==\"%s\"";
  public static final String QUERY_PATTERN_BARCODE = "barcode==\"%s\"";
  public static final String QUERY_PATTERN_CODE = "code==%s";
  public static final String QUERY_PATTERN_USERNAME = "username==%s";
  public static final String QUERY_PATTERN_ADDRESS_TYPE = "addressType==%s";
  public static final String QUERY_PATTERN_GROUP = "group==%s";
  public static final String QUERY_PATTERN_REF_ID = "refId==%s";
  public static final String FIELD_ERROR_MESSAGE_PATTERN = "Field \"%s\" : %s";
  public static final String MSG_NO_CHANGE_REQUIRED = "No change in value required";
  public static final String MSG_NO_ADMINISTRATIVE_CHANGE_REQUIRED =
      "No change in administrative data required";
  public static final String MSG_NO_MARC_CHANGE_REQUIRED = "No change in MARC fields required";
  public static final String MSG_HOLDING_NO_CHANGE_REQUIRED_UNSUPPRESSED_ITEMS_UPDATED =
      "No change in value for holdings record required, associated unsuppressed item(s) "
          + "have been updated.";
  public static final String MSG_HOLDING_NO_CHANGE_REQUIRED_SUPPRESSED_ITEMS_UPDATED =
      "No change in value for holdings record required, associated suppressed item(s) "
          + "have been updated.";
  public static final String STAFF_ONLY = "(staff only)";
  public static final String GET_ITEMS_BY_HOLDING_ID_QUERY = "holdingsRecordId==%s";
  public static final String GET_HOLDINGS_BY_INSTANCE_ID_QUERY = "instanceId==%s";
  public static final String APPLY_TO_HOLDINGS = "APPLY_TO_HOLDINGS";
  public static final String APPLY_TO_ITEMS = "APPLY_TO_ITEMS";
  public static final char NON_PRINTING_DELIMITER = '\u001f';
  public static final String SPECIAL_ARRAY_DELIMITER = NON_PRINTING_DELIMITER + ARRAY_DELIMITER;
  public static final String SPECIAL_ITEM_DELIMITER = NON_PRINTING_DELIMITER + ITEM_DELIMITER;
  public static final String ELECTRONIC_ACCESS_HEADINGS =
      "URL relationship;URI;Link text;Material specified;URL public note\n";
  public static final String SUBJECT_HEADINGS = "Subject headings;Subject source;Subject type\n";
  public static final String PUBLICATION_HEADINGS =
      "Publisher;Publisher role;Place of publication;Publication date\n";
  public static final String CLASSIFICATION_HEADINGS =
      "Classification identifier type;Classification\n";
  public static final String STAFF_ONLY_NOTE_PARAMETER_KEY = "STAFF_ONLY";
  public static final String MSG_ERROR_TEMPLATE_OPTIMISTIC_LOCKING =
      "The record cannot be saved because it is not the most recent version. "
          + "Stored version is %s, bulk edit version is %s.";
  public static final String MSG_ERROR_OPTIMISTIC_LOCKING_DEFAULT =
      "The record cannot be saved because it is not the most recent version.";
  public static final String NO_MARC_CONTENT =
      "Cannot get marc content for record with id = %s, reason: %s";
  public static final String LINKED_DATA_SOURCE_IS_NOT_SUPPORTED =
      "Bulk edit of instances with source set to LINKED_DATA is not supported.";
  public static final String MULTIPLE_SRS =
      "Multiple SRS records are associated with the instance. "
          + "The following SRS have been identified: %s.";
  public static final String SRS_MISSING = "SRS record associated with the instance is missing.";
  public static final String NO_ITEM_VIEW_PERMISSIONS =
      "User %s does not have required permission to view the "
          + "item record - %s=%s on the tenant %s";
  public static final String NO_ITEM_AFFILIATION =
      "User %s does not have required affiliation "
          + "to view the item record - %s=%s on the tenant %s";
  public static final String NO_HOLDING_AFFILIATION =
      "User %s does not have required affiliation to "
          + "view the holdings record - %s=%s on the tenant %s";
  public static final String NO_HOLDING_VIEW_PERMISSIONS =
      "User %s does not have required permission to view the "
          + "holdings record - %s=%s on the tenant %s";
  public static final String DUPLICATES_ACROSS_TENANTS = "Duplicates across tenants";
  public static final String NO_MATCH_FOUND_MESSAGE = "No match found";
  public static final String NO_USER_VIEW_PERMISSIONS =
      "User %s does not have required permission to view "
          + "the user record - %s=%s on the tenant %s";
  public static final String NO_INSTANCE_VIEW_PERMISSIONS =
      "User %s does not have required permission to view the "
          + "instance record - %s=%s on the tenant %s";
  public static final String ERROR_STARTING_BULK_OPERATION = "Error starting Bulk Operation: ";
  public static final String CANNOT_GET_RECORD = "Cannot get data from %s due to %s";
  public static final String MSG_SHADOW_RECORDS_CANNOT_BE_EDITED =
      "Shadow records cannot be bulk edited.";

  public static final String CSV_MSG_ERROR_TEMPLATE_OPTIMISTIC_LOCKING =
      "The record cannot be saved because it is not the most recent version. "
          + "Stored version is %s, bulk edit version is %s.";
  public static final String RECORD_CANNOT_BE_UPDATED_ERROR_TEMPLATE =
      "%s cannot be updated because the record is associated with %s and "
          + "%s is not associated with this tenant.";
  public static final String DATA_IMPORT_ERROR_DISCARDED =
      "An error occurred during "
          + "the update operation, possibly due to multiple MARC records linked to the "
          + "same instance. Please review the inventory for potential data inconsistencies.";
  public static final String ITEM_TYPE = "ITEM";
  public static final String HOLDING_TYPE = "HOLDINGS_RECORD";
  public static final Set<String> SPLIT_NOTE_ENTITIES = Set.of(ITEM_TYPE, HOLDING_TYPE);
  public static final String LINKED_DATA_SOURCE = "LINKED_DATA";
  public static final String ENTITY = "entity";
  public static final String TENANT_ID = "tenantId";

  public static final String FIXED_FIELD = "008";
  public static final int FIXED_FIELD_LENGTH = 40;
  public static final String FIELD_999 = "999";
  public static final char INDICATOR_F = 'f';

  public static final String CSV_EXTENSION = "csv";

  public static final String MARC = "MARC";
  public static final String FOLIO = "FOLIO";
  public static final String CONSORTIUM_MARC = "CONSORTIUM-MARC";
  public static final String CONSORTIUM_FOLIO = "CONSORTIUM-FOLIO";
  public static final String ERROR_COMMITTING_FILE_NAME_PREFIX = "-Committing-changes-Errors-";
  public static final String ERROR_MATCHING_FILE_NAME_PREFIX = "-Matching-Records-Errors-";
  public static final String ERROR_FILE_NAME_ENDING = "-Errors.csv";
  public static final String ENRICHED_PREFIX = "enriched-";
  public static final String CHANGED_CSV_PATH_TEMPLATE = "%s/%s-Changed-Records-CSV-%s.csv";
  public static final String CHANGED_MARC_PATH_TEMPLATE = "%s/%s-Changed-Records-MARC-%s.mrc";
  public static final String CHANGED_MARC_CSV_PATH_TEMPLATE =
      "%s/%s-Changed-Records-MARC-CSV-%s.csv";
  public static final String MATCHED_RECORDS_FILE_TEMPLATE = "%s/%s%s-%s-Records-%s.%s";

  public static final byte[] UTF_8_BOM = new byte[] {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
  public static final String UTF8_BOM =
      new String(new byte[] {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF}, StandardCharsets.UTF_8);

  // from mod-data-export-worker
  public static final String BULK_EDIT_IDENTIFIERS = "BULK_EDIT_IDENTIFIERS";
  public static final String FILE_NAME = "fileName";
  public static final String IDENTIFIERS_FILE_NAME = "identifiersFileName";

  public static final String MULTIPLE_MATCHES_MESSAGE = "Multiple matches for the same identifier.";
  public static final String DUPLICATE_ENTRY = "Duplicate entry";
  public static final String FILE_UPLOAD_ERROR =
      "File uploading failed : Cannot upload a file. Reason: %s.";

  public static final int MIN_YEAR_FOR_BIRTH_DATE = 1900;

  public static final String TOTAL_CSV_LINES = "totalCsvLines";
  public static final String NUMBER_OF_PROCESSED_IDENTIFIERS = "numberOfProcessedIdentifiers";
  public static final String NUMBER_OF_MATCHED_RECORDS = "numberOfMatchedRecords";

  public static final String MATCHED_RECORDS_PATH_TEMPLATE = "%s/%s-Matched-Records-%s";
  public static final String MARC_RECORDS_PATH_TEMPLATE = "%s/%s-Marc-Records-%s";
  public static final String PERMANENT_LOCATION_ID = "permanentLocationId";
  public static final String IS_ACTIVE = "isActive";
  public static final String INACTIVE = "Inactive ";
  public static final String NAME = "name";
  public static final String CALL_NUMBER_PREFIX = "callNumberPrefix";
  public static final String CALL_NUMBER = "callNumber";
  public static final String CALL_NUMBER_SUFFIX = "callNumberSuffix";
  public static final String HOLDINGS_LOCATION_CALL_NUMBER_DELIMITER = " > ";

  /* Entities json keys for mapping from FQM query response */
  public static final String ID = "id";
  public static final String TITLE = "title";
  public static final String HOLDINGS_DATA = "holdingsData";
  public static final String INSTANCE_TITLE = "instanceTitle";
  public static final String CHILD_INSTANCES = "childInstances";
  public static final String PARENT_INSTANCES = "parentInstances";
  public static final String PRECEDING_TITLES = "precedingTitles";
  public static final String SUCCEEDING_TITLES = "succeedingTitles";
  public static final String PERMANENT_LOAN_TYPE = "permanentLoanType";
  public static final String TEMPORARY_LOAN_TYPE = "temporaryLoanType";
  public static final String EFFECTIVE_LOCATION = "effectiveLocation";
}
