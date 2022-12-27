package org.folio.bulkops.configs;

import static feign.FeignException.errorStatus;

import org.folio.bulkops.error.NotFoundException;
import org.springframework.http.HttpStatus;

import feign.Response;
import feign.codec.ErrorDecoder;

public class CustomFeignErrorDecoder implements ErrorDecoder {

  @Override
  public Exception decode(String methodKey, Response response) {
    String requestUrl = response.request().url();
    if (HttpStatus.NOT_FOUND.value() == response.status()) {
      return new NotFoundException(requestUrl);
    }
    return errorStatus(methodKey, response);
  }
}
