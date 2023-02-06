package org.folio.bulkops.configs;

import static feign.FeignException.errorStatus;

import org.folio.bulkops.exception.BadRequestException;
import org.folio.bulkops.exception.NotFoundException;
import org.folio.bulkops.exception.ServerErrorException;
import org.springframework.http.HttpStatus;

import feign.Response;
import feign.codec.ErrorDecoder;

import java.io.IOException;
import java.util.Arrays;

public class CustomFeignErrorDecoder implements ErrorDecoder {

  @Override
  public Exception decode(String methodKey, Response response) {
    String requestUrl = response.request().url();
    if (HttpStatus.NOT_FOUND.value() == response.status()) {
      return new NotFoundException("Not found: " + requestUrl);
    } else if (HttpStatus.BAD_REQUEST.value() == response.status()) {
      try (var bodyIs = response.body().asInputStream()) {
        return new BadRequestException("Bad request: " + requestUrl + ", message: " + Arrays.toString(bodyIs.readAllBytes()));
      } catch (IOException e) {
        return new BadRequestException("Bad request: " + requestUrl);
      }
    } else if (HttpStatus.INTERNAL_SERVER_ERROR.value() == response.status()) {
      return new ServerErrorException(requestUrl);
    }
    return errorStatus(methodKey, response);
  }
}
