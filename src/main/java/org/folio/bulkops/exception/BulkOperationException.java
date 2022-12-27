package org.folio.bulkops.exception;

public class BulkOperationException extends Exception {
  public BulkOperationException(String message) {
    super(message.replace(',', '_'));
  }

  public BulkOperationException(BulkOperationException e) {
    super(e);
  }
}
