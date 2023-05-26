package org.folio.bulkops.exception;

import com.opencsv.exceptions.CsvRuntimeException;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.lang.reflect.Field;

@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@Data
public class ConverterException extends CsvRuntimeException {
  private Field field;
  private Object value;
  private String message;
}
