package org.folio.bulkops.exception;

public class ProfileLockedException extends RuntimeException {
  public ProfileLockedException(String message) {
    super(message);
  }
}
