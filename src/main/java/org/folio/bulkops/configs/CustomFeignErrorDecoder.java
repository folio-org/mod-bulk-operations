package org.folio.bulkops.configs;

import static feign.FeignException.errorStatus;

import org.folio.bulkops.exception.BadRequestException;
import org.folio.bulkops.exception.NotFoundException;
import org.folio.bulkops.exception.ServerErrorException;
import org.springframework.http.HttpStatus;

import feign.Response;
import feign.codec.ErrorDecoder;

public class CustomFeignErrorDecoder implements ErrorDecoder {

  @Override
  public Exception decode(String methodKey, Response response) {
    String requestUrl = response.request().url();
    if (HttpStatus.NOT_FOUND.value() == response.status()) {
      return new NotFoundException(requestUrl);
    } else if (HttpStatus.BAD_REQUEST.value() == response.status()) {
      return new BadRequestException(requestUrl);
    } else if (HttpStatus.INTERNAL_SERVER_ERROR.value() == response.status()) {
      return new ServerErrorException(requestUrl);
    }
    return errorStatus(methodKey, response);
  }
}
