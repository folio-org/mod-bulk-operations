package org.folio.bulkops.exception;

import com.opencsv.exceptions.CsvRuntimeException;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.folio.bulkops.domain.bean.BulkOperationsEntity;
import org.folio.bulkops.domain.dto.ErrorType;
import org.folio.bulkops.domain.entity.BulkOperation;

import java.lang.reflect.Field;

@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@Data
public class ConverterException extends CsvRuntimeException {
  private final transient Field field;
  private final transient Object value;
  private final String message;
  private final ErrorType errorType;
}
