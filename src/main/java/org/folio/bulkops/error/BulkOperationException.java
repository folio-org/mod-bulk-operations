package org.folio.bulkops.error;

public class BulkOperationException extends RuntimeException {
  public BulkOperationException(String message) {
    super(message.replace(',', '_'));
  }
}
