package org.folio.bulkops.exception;

public class OptimisticLockingException extends RuntimeException {
  public OptimisticLockingException(String message) {
    super(message);
  }
}
