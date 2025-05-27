package org.folio.bulkops.domain.bean;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class JobParameterNames {

  public static final String BULK_OPERATION_ID = "bulkOperationId";
  public static final String UNIQUE_EXECUTION_ID = "uniqueExecutionId";
  public static final String TEMP_LOCAL_FILE_PATH = "tempLocalFilePath";
  public static final String STORAGE_FILE_PATH = "storageFilePath";
  public static final String TEMP_LOCAL_MARC_PATH = "tempLocalMarcPath";
  public static final String STORAGE_MARC_PATH = "storageMarcPath";
  public static final String TEMP_OUTPUT_CSV_PATH = "tempOutputCsvPath";
  public static final String TEMP_OUTPUT_JSON_PATH = "tempOutputJsonPath";
  public static final String TEMP_OUTPUT_MARC_PATH = "tempOutputMarcPath";
  public static final String AT_LEAST_ONE_MARC_EXISTS = "atLeastOneMarcExists";
  public static final String IDENTIFIER_TYPE = "identifierType";
  public static final String ENTITY_TYPE = "entityType";
}
