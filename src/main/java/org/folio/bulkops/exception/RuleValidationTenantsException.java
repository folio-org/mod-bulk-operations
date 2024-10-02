package org.folio.bulkops.exception;

import lombok.Getter;

@Getter
public class RuleValidationTenantsException extends Exception {

  private final String identifier;

  public RuleValidationTenantsException(String message, String identifier) {
    super(message);
    this.identifier = identifier;
  }
}
