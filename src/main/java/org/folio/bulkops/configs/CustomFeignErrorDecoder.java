package org.folio.bulkops.configs;

import feign.Response;
import feign.codec.ErrorDecoder;
import lombok.extern.log4j.Log4j2;
import org.folio.bulkops.exception.BadRequestException;
import org.folio.bulkops.exception.NotFoundException;
import org.folio.bulkops.exception.ServerErrorException;
import org.springframework.boot.configurationprocessor.json.JSONException;
import org.springframework.boot.configurationprocessor.json.JSONObject;
import org.springframework.http.HttpStatus;

import java.io.IOException;

import static feign.FeignException.errorStatus;

@Log4j2
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
    } else if (msg.startsWith("This barcode has already been taken")) {
      var body = new String(response.request().body());
      String barcode = "unavailable";
      try {
        barcode = new JSONObject(body).getString("barcode");
      } catch (JSONException e) {
        log.error("Not unique barcode cannot be retrieved from the request body: {}", body);
      }
      finalMsg = "Error reads: " + barcode + " barcode is not unique";
    }
    return finalMsg;
  }
}
