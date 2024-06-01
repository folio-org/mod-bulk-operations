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
   * @deprecated This method is not supported. {@link this.getCsvErrorMessage()}
   * and {@link this.getUiErrorMessage()} should be used instead.
   */
  @Override
  @Deprecated
  public String getMessage() {
    throw new UnsupportedOperationException("Not supported");
  }
}
