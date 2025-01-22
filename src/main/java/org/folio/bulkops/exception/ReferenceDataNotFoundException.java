package org.folio.bulkops.exception;

import org.folio.bulkops.domain.dto.ErrorType;

public class ReferenceDataNotFoundException extends ConverterException {

  public ReferenceDataNotFoundException(String message) {
    super(null, null, message, ErrorType.WARNING);
  }
}
