package org.folio.bulkops.config;

import static feign.FeignException.errorStatus;

import feign.Response;
import feign.codec.ErrorDecoder;
import org.folio.bulkops.exception.NotFoundException;
import org.folio.bulkops.exception.UpdateConflictException;
import org.springframework.http.HttpStatus;

public class CustomFeignErrorDecoder implements ErrorDecoder {

  @Override
  public Exception decode(String methodKey, Response response) {
    String requestUrl = response.request().url();
    if (HttpStatus.NOT_FOUND.value() == response.status()) {
      return new NotFoundException(requestUrl);
    } else if (HttpStatus.CONFLICT.value() == response.status()) {
      return new UpdateConflictException(requestUrl);
    }
    return errorStatus(methodKey, response);
  }

}
