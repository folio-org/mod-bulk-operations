package org.folio.bulkops.configs;

import static feign.FeignException.errorStatus;

import java.io.IOException;

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
      return new NotFoundException("Not found: " + requestUrl);
    } else if (HttpStatus.BAD_REQUEST.value() == response.status()) {
      try (var bodyIs = response.body().asInputStream()) {
        var msg = new String(bodyIs.readAllBytes());
        return new BadRequestException(updateErrorMessage(msg, response));
      } catch (IOException e) {
        return new BadRequestException("Bad request: " + requestUrl);
      }
    } else if (HttpStatus.INTERNAL_SERVER_ERROR.value() == response.status()) {
      return new ServerErrorException(requestUrl);
    }
    return errorStatus(methodKey, response);
  }

  private String updateErrorMessage(String msg, Response response) {
    String finalMsg = msg;
    if (msg.startsWith("Error at index")) {
      var url = response.request().url();
      finalMsg = "Invalid user UUID: " + url.substring(url.lastIndexOf("/") + 1);
    }
    return finalMsg;
  }
}
