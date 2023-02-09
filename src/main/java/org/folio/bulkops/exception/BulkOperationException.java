package org.folio.bulkops.exception;

public class BulkOperationException extends Exception {
  public BulkOperationException(String message) {
    super(message.replace(',', '_'));
  }
}
