package org.folio.bulkops.exception;

public class WritePermissionDoesNotExist extends RuntimeException {
  public WritePermissionDoesNotExist(String message) {
    super(message);
  }
}
