package org.folio.bulkops.exception;

public class ReadPermissionException extends UploadFromQueryException {
  public ReadPermissionException(String message, String identifier) {
    super(message, identifier);
  }
}
