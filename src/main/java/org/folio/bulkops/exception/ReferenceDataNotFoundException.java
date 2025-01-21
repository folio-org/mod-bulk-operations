package org.folio.bulkops.exception;

import lombok.Data;
import org.folio.bulkops.domain.dto.ErrorType;

@Data
public class ReferenceDataNotFoundException extends ConverterException {

  public ReferenceDataNotFoundException(String message) {
    super(message, ErrorType.WARNING);
  }
}
