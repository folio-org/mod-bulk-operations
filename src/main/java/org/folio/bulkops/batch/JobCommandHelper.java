package org.folio.bulkops.batch;

import static org.folio.bulkops.domain.bean.JobParameterNames.BULK_OPERATION_ID;
import static org.folio.bulkops.domain.bean.JobParameterNames.ENTITY_TYPE;
import static org.folio.bulkops.domain.bean.JobParameterNames.IDENTIFIER_TYPE;
import static org.folio.bulkops.domain.bean.JobParameterNames.STORAGE_FILE_PATH;
import static org.folio.bulkops.domain.bean.JobParameterNames.STORAGE_MARC_PATH;
import static org.folio.bulkops.domain.bean.JobParameterNames.TEMP_LOCAL_FILE_PATH;
import static org.folio.bulkops.domain.bean.JobParameterNames.TEMP_LOCAL_MARC_PATH;
import static org.folio.bulkops.domain.bean.JobParameterNames.UNIQUE_EXECUTION_ID;
import static org.folio.bulkops.util.Constants.FILE_NAME;
import static org.folio.bulkops.util.Constants.IDENTIFIERS_FILE_NAME;
import static org.folio.bulkops.util.Constants.MARC_RECORDS_PATH_TEMPLATE;
import static org.folio.bulkops.util.Constants.MATCHED_RECORDS_PATH_TEMPLATE;
import static org.folio.bulkops.util.Constants.SLASH;
import static org.folio.bulkops.util.Constants.TOTAL_CSV_LINES;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.UUID;
import lombok.experimental.UtilityClass;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FilenameUtils;
import org.folio.bulkops.domain.entity.BulkOperation;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;

@UtilityClass
@Log4j2
public class JobCommandHelper {
  private static final boolean JOB_PARAMETER_DEFAULT_IDENTIFYING_VALUE = false;

  public static JobParameters prepareJobParameters(BulkOperation bulkOperation, int numOfLines) {
    createTmpWorkDir(bulkOperation);

    var baseFileName =
        FilenameUtils.getBaseName(bulkOperation.getLinkToTriggeringCsvFile());

    var fileName = MATCHED_RECORDS_PATH_TEMPLATE
        .formatted(
            bulkOperation.getId(),
            LocalDate.now(),
            baseFileName
        );

    var marcFileName = MARC_RECORDS_PATH_TEMPLATE
        .formatted(
            bulkOperation.getId(),
            LocalDate.now(),
            baseFileName
        );

    var paramsBuilder = new JobParametersBuilder();

    paramsBuilder.addString(BULK_OPERATION_ID, bulkOperation.getId().toString());
    paramsBuilder.addString(UNIQUE_EXECUTION_ID, UUID.randomUUID().toString());

    paramsBuilder.addString(
        IDENTIFIERS_FILE_NAME,
        bulkOperation.getLinkToTriggeringCsvFile(),
        JOB_PARAMETER_DEFAULT_IDENTIFYING_VALUE
    );

    paramsBuilder.addString(
        FILE_NAME,
        bulkOperation.getLinkToTriggeringCsvFile(),
        JOB_PARAMETER_DEFAULT_IDENTIFYING_VALUE
    );

    paramsBuilder.addLong(
        TOTAL_CSV_LINES,
        (long) numOfLines,
        JOB_PARAMETER_DEFAULT_IDENTIFYING_VALUE
    );

    paramsBuilder.addString(
        TEMP_LOCAL_FILE_PATH,
        getWorkDir() + fileName,
        JOB_PARAMETER_DEFAULT_IDENTIFYING_VALUE
    );

    paramsBuilder.addString(
        STORAGE_FILE_PATH,
        fileName,
        JOB_PARAMETER_DEFAULT_IDENTIFYING_VALUE
    );

    paramsBuilder.addString(
        TEMP_LOCAL_MARC_PATH,
        getWorkDir() + marcFileName,
        JOB_PARAMETER_DEFAULT_IDENTIFYING_VALUE
    );

    paramsBuilder.addString(
        STORAGE_MARC_PATH,
        marcFileName,
        JOB_PARAMETER_DEFAULT_IDENTIFYING_VALUE
    );

    paramsBuilder.addString(
        IDENTIFIER_TYPE,
        bulkOperation.getIdentifierType().getValue(),
        JOB_PARAMETER_DEFAULT_IDENTIFYING_VALUE
    );

    paramsBuilder.addString(
        ENTITY_TYPE,
        bulkOperation.getEntityType().getValue(),
        JOB_PARAMETER_DEFAULT_IDENTIFYING_VALUE
    );

    return paramsBuilder.toJobParameters();
  }

  private static String getWorkDir() {
    var dir = System.getProperty("java.io.tmpdir");
    return dir.endsWith(SLASH) ? dir : dir + SLASH;
  }

  private static void createTmpWorkDir(BulkOperation bulkOperation) {
    var path = Path.of(getWorkDir() + SLASH + bulkOperation.getId());
    try {
      Files.createDirectories(path);
    } catch (IOException e) {
      log.error("Failed to create temporary working directory", e);
    }
  }
}
