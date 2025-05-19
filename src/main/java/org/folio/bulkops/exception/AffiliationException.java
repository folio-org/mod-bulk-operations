package org.folio.bulkops.exception;

public class AffiliationException extends UploadFromQueryException {

  public AffiliationException(String message, String identifier) {
    super(message, identifier);
  }
}
