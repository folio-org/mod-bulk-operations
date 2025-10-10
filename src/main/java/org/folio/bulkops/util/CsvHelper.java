package org.folio.bulkops.util;

import static java.lang.String.format;
import static org.folio.bulkops.domain.dto.OperationStatusType.APPLY_CHANGES;
import static org.folio.bulkops.util.Constants.FIELD_ERROR_MESSAGE_PATTERN;

import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;
import java.util.List;
import lombok.Getter;
import lombok.experimental.UtilityClass;
import lombok.extern.log4j.Log4j2;
import org.folio.bulkops.domain.bean.BulkOperationsEntity;
import org.folio.bulkops.domain.bean.StateType;
import org.folio.bulkops.domain.entity.BulkOperation;
import org.folio.bulkops.domain.entity.BulkOperationExecutionContent;
import org.folio.bulkops.exception.ConverterException;

@UtilityClass
@Log4j2
public class CsvHelper {
  private static final char ASCII_ZERO_CHAR = '\0';
  @Getter
  private static final CSVParser csvParser;

  static {
    csvParser = new CSVParserBuilder().withEscapeChar(ASCII_ZERO_CHAR).build();
  }

  public static void writeBeanToCsv(BulkOperation operation,
                                    BulkOperationsEntityCsvWriter csvWriter,
                                    BulkOperationsEntity bean,
                                    List<BulkOperationExecutionContent>
                                            bulkOperationExecutionContents)
          throws CsvRequiredFieldEmptyException, CsvDataTypeMismatchException {
    try {
      csvWriter.write(bean);
    } catch (ConverterException e) {
      if (APPLY_CHANGES.equals(operation.getStatus())) {
        log.error("Record {}, field: {}, converter exception: {}",
                bean.getIdentifier(operation.getIdentifierType()), e.getField().getName(),
                e.getMessage());
      } else {
        bulkOperationExecutionContents.add(BulkOperationExecutionContent.builder()
                .identifier(bean.getIdentifier(operation.getIdentifierType()))
                .bulkOperationId(operation.getId())
                .state(StateType.FAILED)
                .errorType(e.getErrorType())
                .errorMessage(format(FIELD_ERROR_MESSAGE_PATTERN, e.getField().getName(),
                        e.getMessage()))
                .build());
      }
      writeBeanToCsv(operation, csvWriter, bean, bulkOperationExecutionContents);
    }
  }
}
