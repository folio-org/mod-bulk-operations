package org.folio.bulkops.exception;

public class UpdateConflictException extends RuntimeException {
  public UpdateConflictException(String message) {
    super(message);
  }
}
