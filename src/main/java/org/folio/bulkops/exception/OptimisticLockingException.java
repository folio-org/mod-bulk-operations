package org.folio.bulkops.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class OptimisticLockingException extends RuntimeException {

  private final String csvErrorMessage;
  private final String uiErrorMessage;
  private final String linkToFailedEntity;

  /**
   * This method is unsupported and should not be used.
   *
   * @deprecated Use {@link #getCsvErrorMessage()} and {@link #getUiErrorMessage()} instead.
   */
  @Override
  @Deprecated
  public String getMessage() {
    throw new UnsupportedOperationException("Not supported");
  }
}
