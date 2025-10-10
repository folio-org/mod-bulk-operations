package org.folio.bulkops.exception;

import com.opencsv.exceptions.CsvRuntimeException;
import java.lang.reflect.Field;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.folio.bulkops.domain.dto.ErrorType;

@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@Data
public class ConverterException extends CsvRuntimeException {
  private final transient Field field;
  private final transient Object value;
  private final String message;
  private final ErrorType errorType;
}
