package org.folio.bulkops.exception;

import lombok.Getter;
import org.folio.bulkops.domain.dto.ErrorType;

public class UploadFromQueryException extends Exception {

  @Getter
  private final ErrorType errorType;
  @Getter
  private final String identifier;

  public UploadFromQueryException(String message) {
    super(message);
    errorType = ErrorType.ERROR;
    identifier = null;
  }

  public UploadFromQueryException(String message, String identifier) {
    super(message);
    errorType = ErrorType.ERROR;
    this.identifier = identifier;
  }
}
