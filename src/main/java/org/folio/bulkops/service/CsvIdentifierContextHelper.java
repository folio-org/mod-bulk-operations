package org.folio.bulkops.service;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.bulkops.exception.ConverterException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Log4j2
public class CsvIdentifierContextHelper implements InitializingBean {

  private final ErrorService errorService;

  public void saveError(UUID bulkOperationId, String identifier,
                        ConverterException converterException) {
    errorService.saveError(bulkOperationId, identifier, converterException.getMessage(),
            converterException.getErrorType());
  }

  private static CsvIdentifierContextHelper service;

  @Override
  public void afterPropertiesSet() {
    service = this;
  }

  public static CsvIdentifierContextHelper service() {
    return service;
  }
}
