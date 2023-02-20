package org.folio.bulkops.exception;

public class ServerErrorException extends RuntimeException {
  public ServerErrorException(String message) {
    super(message);
  }

  public ServerErrorException(String message, Throwable e) {
    super(message, e);
  }
}
