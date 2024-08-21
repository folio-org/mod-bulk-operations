package org.folio.bulkops.exception;

public class DataImportException extends RuntimeException {
  public DataImportException(Exception exc) {
    super(exc);
  }
}
