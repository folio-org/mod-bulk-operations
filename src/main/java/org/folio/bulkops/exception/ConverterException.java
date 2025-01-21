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
@NoArgsConstructor
@Data
public class ConverterException extends CsvRuntimeException {
  private transient Field field;
  private transient Object value;
  private String message;
  private BulkOperationsEntity bean;
  private BulkOperation bulkOperation;
  private ErrorType errorType;
  public ConverterException(Field field, Object value, String message) {
    this.field = field;
    this.value = value;
    this.message = message;
  }

  public ConverterException(Field field, Object value, String message, ErrorType errorType) {
    this(message, errorType);
    this.field = field;
    this.value = value;
  }

  public ConverterException(String message, ErrorType errorType) {
    this.message = message;
    this.errorType = errorType;
  }
}
