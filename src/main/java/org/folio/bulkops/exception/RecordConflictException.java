package org.folio.bulkops.exception;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@Data
public class RecordConflictException extends RuntimeException {
  public RecordConflictException(String message) {
    super(message);
  }
}
