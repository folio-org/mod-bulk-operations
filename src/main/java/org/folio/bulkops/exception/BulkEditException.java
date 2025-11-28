package org.folio.bulkops.exception;

import org.folio.bulkops.domain.dto.ErrorType;

public class BulkEditException extends RuntimeException {

  private final ErrorType errorType;

  public BulkEditException(String message, ErrorType errorType) {
    super(message.replace(',', '_'));
    this.errorType = errorType;
  }

  public BulkEditException(String message) {
    super(message);
    errorType = ErrorType.ERROR;
  }

  public ErrorType getErrorType() {
    return errorType;
  }
}
