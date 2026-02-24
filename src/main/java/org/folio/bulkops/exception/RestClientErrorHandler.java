package org.folio.bulkops.exception;

import static org.folio.bulkops.util.Constants.CANNOT_GET_RECORD;

import java.io.IOException;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;

@Component
public class RestClientErrorHandler {

  public void handle(HttpRequest request, ClientHttpResponse response) throws IOException {
    int status = response.getStatusCode().value();

    switch (status) {
      case 400 -> handle400(request, response);
      case 404 -> handle404(request);
      default -> handleOtherError(request, response);
    }
  }

  private void handle404(HttpRequest request) {
    throw new NotFoundException("Not found: " + request.getURI());
  }

  private void handle400(HttpRequest request, ClientHttpResponse response) {
    try (var bodyIs = response.getBody()) {
      var msg = new String(bodyIs.readAllBytes());
      throw new BadRequestException(updateErrorMessage(msg, request.getURI().toString()));
    } catch (IOException e) {
      throw new BadRequestException("Bad request: " + request.getURI());
    }
  }

  private void handleOtherError(HttpRequest request, ClientHttpResponse response) {
    try (var bodyIs = response.getBody()) {
      var msg = new String(bodyIs.readAllBytes());
      String reason = !msg.isBlank() ? msg : "Unknown error";
      throw new BulkEditException(
          CANNOT_GET_RECORD.formatted(request.getURI(), reason),
          org.folio.bulkops.domain.dto.ErrorType.ERROR);
    } catch (IOException e) {
      throw new BulkEditException("Unable to get reason for error: " + e.getMessage());
    }
  }

  private String updateErrorMessage(String msg, String url) {
    String finalMsg = msg;
    if (msg.startsWith("Error at index")) {
      finalMsg = "Invalid user UUID: " + url.substring(url.lastIndexOf("/") + 1);
    }
    return finalMsg;
  }
}
