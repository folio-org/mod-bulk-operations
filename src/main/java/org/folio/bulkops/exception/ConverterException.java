package org.folio.bulkops.exception;

import com.opencsv.exceptions.CsvRuntimeException;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.lang.reflect.Field;

@AllArgsConstructor
@Data
public class ConverterException extends CsvRuntimeException {
  private Field field;
  private Object value;
  private String message;
}
