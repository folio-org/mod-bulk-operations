package org.folio.bulkops.configs;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.folio.bulkops.domain.dto.ErrorType;
import org.folio.bulkops.exception.BadRequestException;
import org.folio.bulkops.exception.BulkEditException;
import org.folio.bulkops.exception.NotFoundException;
import org.folio.bulkops.exception.RestClientErrorHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class HttpClientConfigurationTest {

  private WireMockServer wireMockServer;
  private RestClient restClient;

  @BeforeEach
  void setUp() {
    wireMockServer = new WireMockServer(0);
    wireMockServer.start();

    HttpClientConfiguration httpClientConfiguration =
        new HttpClientConfiguration(new RestClientErrorHandler());
    restClient = httpClientConfiguration.restClient(RestClient.builder());
  }

  @AfterEach
  void tearDown() {
    if (wireMockServer != null && wireMockServer.isRunning()) {
      wireMockServer.stop();
    }
  }

  @Test
  void testRestClient404StatusHandlerThrowsNotFoundException() {
    String endpoint = "/users/123";
    wireMockServer.stubFor(get(urlEqualTo(endpoint)).willReturn(aResponse().withStatus(404)));

    String url = wireMockServer.baseUrl() + endpoint;

    NotFoundException exception =
        assertThrows(NotFoundException.class, () -> getBody(url));

    assertEquals("Not found: " + url, exception.getMessage());
  }

  @Test
  void testRestClient400StatusHandlerWithNormalErrorMessage() {
    String endpoint = "/api/items";
    String errorMessage = "Validation failed: invalid item data";
    wireMockServer.stubFor(
        get(urlEqualTo(endpoint)).willReturn(aResponse().withStatus(400).withBody(errorMessage)));

    String url = wireMockServer.baseUrl() + endpoint;

    BadRequestException exception =
        assertThrows(BadRequestException.class, () -> getBody(url));

    assertEquals(errorMessage, exception.getMessage());
  }

  @Test
  void testRestClient400StatusHandlerWithErrorAtIndexMessage() {
    String endpoint = "/users/invalid-uuid-123";
    String errorMessage = "Error at index 0: Invalid UUID format";
    wireMockServer.stubFor(
        get(urlEqualTo(endpoint)).willReturn(aResponse().withStatus(400).withBody(errorMessage)));

    String url = wireMockServer.baseUrl() + endpoint;

    BadRequestException exception =
        assertThrows(BadRequestException.class, () -> getBody(url));

    assertEquals("Invalid user UUID: invalid-uuid-123", exception.getMessage());
  }

  @Test
  void testRestClient400StatusHandlerWithErrorAtIndexMessageExtractsLastPathSegment() {
    String endpoint = "/api/v1/users/some-uuid-value";
    String errorMessage = "Error at index 5: invalid format";
    wireMockServer.stubFor(
        get(urlEqualTo(endpoint)).willReturn(aResponse().withStatus(400).withBody(errorMessage)));

    String url = wireMockServer.baseUrl() + endpoint;

    BadRequestException exception =
        assertThrows(BadRequestException.class, () -> getBody(url));

    assertEquals("Invalid user UUID: some-uuid-value", exception.getMessage());
  }

  @Test
  void testRestClient400StatusHandlerWithEmptyBody() {
    String endpoint = "/users";
    wireMockServer.stubFor(
        get(urlEqualTo(endpoint)).willReturn(aResponse().withStatus(400).withBody("")));

    String url = wireMockServer.baseUrl() + endpoint;

    BadRequestException exception =
        assertThrows(BadRequestException.class, () -> getBody(url));

    assertEquals("", exception.getMessage());
  }

  @Test
  void testRestClient500StatusHandlerWithErrorMessage() {
    String endpoint = "/api/resource";
    String errorMessage = "Internal server error occurred";
    wireMockServer.stubFor(
        get(urlEqualTo(endpoint)).willReturn(aResponse().withStatus(500).withBody(errorMessage)));

    String url = wireMockServer.baseUrl() + endpoint;

    BulkEditException exception =
        assertThrows(BulkEditException.class, () -> getBody(url));

    assertEquals("Cannot get data from " + url + " due to " + errorMessage, exception.getMessage());
    assertEquals(ErrorType.ERROR, exception.getErrorType());
  }

  @Test
  void testRestClient502StatusHandlerWithBlankMessage() {
    String endpoint = "/api/resource";
    String blankMessage = "   ";
    wireMockServer.stubFor(
        get(urlEqualTo(endpoint)).willReturn(aResponse().withStatus(502).withBody(blankMessage)));

    String url = wireMockServer.baseUrl() + endpoint;

    BulkEditException exception =
        assertThrows(BulkEditException.class, () -> getBody(url));

    assertEquals("Cannot get data from " + url + " due to Unknown error", exception.getMessage());
    assertEquals(ErrorType.ERROR, exception.getErrorType());
  }

  @Test
  void testRestClient503StatusHandlerWithEmptyMessage() {
    String endpoint = "/api/resource";
    wireMockServer.stubFor(
        get(urlEqualTo(endpoint)).willReturn(aResponse().withStatus(503).withBody("")));

    String url = wireMockServer.baseUrl() + endpoint;

    BulkEditException exception =
        assertThrows(BulkEditException.class, () -> getBody(url));

    assertEquals("Cannot get data from " + url + " due to Unknown error", exception.getMessage());
    assertEquals(ErrorType.ERROR, exception.getErrorType());
  }

  @Test
  void testRestClient504StatusHandlerWithGatewayTimeout() {
    String endpoint = "/api/resource";
    String errorMessage = "Gateway Timeout";
    wireMockServer.stubFor(
        get(urlEqualTo(endpoint)).willReturn(aResponse().withStatus(504).withBody(errorMessage)));

    String url = wireMockServer.baseUrl() + endpoint;

    BulkEditException exception =
        assertThrows(BulkEditException.class, () -> getBody(url));

    assertEquals("Cannot get data from " + url + " due to " + errorMessage, exception.getMessage());
    assertEquals(ErrorType.ERROR, exception.getErrorType());
  }

  @Test
  void testRestClientMultipleErrorCodesAllTriggerErrorHandler() {
    int[] errorCodes = {500, 501, 502, 503, 504, 505};

    for (int code : errorCodes) {
      String endpoint = "/api/error" + code;
      String errorMessage = "Error code " + code;

      wireMockServer.stubFor(
          get(urlEqualTo(endpoint))
              .willReturn(aResponse().withStatus(code).withBody(errorMessage)));

      String url = wireMockServer.baseUrl() + endpoint;

      BulkEditException exception =
          assertThrows(BulkEditException.class, () -> getBody(url));

      assertEquals(
          "Cannot get data from " + url + " due to Error code " + code, exception.getMessage());
      assertEquals(ErrorType.ERROR, exception.getErrorType());
    }
  }

  @Test
  void testRestClientErrorHandlerHandlesNullBody() {
    String endpoint = "/api/null-body";
    wireMockServer.stubFor(get(urlEqualTo(endpoint)).willReturn(aResponse().withStatus(500)));

    String url = wireMockServer.baseUrl() + endpoint;

    BulkEditException exception =
        assertThrows(BulkEditException.class, () -> getBody(url));

    assertTrue(exception.getMessage().contains("Unknown error"));
  }

  @Test
  void testUpdateErrorMessageWithErrorAtIndexExtractsUuidFromPath() {
    String endpoint = "/users/abc-def-ghi-123";
    String errorMessage = "Error at index 0: some error";

    wireMockServer.stubFor(
        get(urlEqualTo(endpoint)).willReturn(aResponse().withStatus(400).withBody(errorMessage)));

    String url = wireMockServer.baseUrl() + endpoint;

    BadRequestException exception =
        assertThrows(BadRequestException.class, () -> getBody(url));

    assertEquals("Invalid user UUID: abc-def-ghi-123", exception.getMessage());
  }

  @Test
  void testUpdateErrorMessageWithoutErrorAtIndexPassesMessageThrough() {
    String endpoint = "/users";
    String errorMessage = "Regular validation error";

    wireMockServer.stubFor(
        get(urlEqualTo(endpoint)).willReturn(aResponse().withStatus(400).withBody(errorMessage)));

    String url = wireMockServer.baseUrl() + endpoint;

    BadRequestException exception =
        assertThrows(BadRequestException.class, () -> getBody(url));

    assertEquals(errorMessage, exception.getMessage());
  }

  @Test
  void testRestClientSuccessfulRequestDoesNotThrowException() {
    String endpoint = "/api/success";
    String responseBody = "Success response";
    wireMockServer.stubFor(
        get(urlEqualTo(endpoint)).willReturn(aResponse().withStatus(200).withBody(responseBody)));

    String url = wireMockServer.baseUrl() + endpoint;

    String result = getBody(url);
    assertEquals(responseBody, result);
  }

  @Test
  void testRestClient404HandlerIsInvokedBeforeGenericErrorHandler() {
    String endpoint = "/not-found";
    wireMockServer.stubFor(get(urlEqualTo(endpoint)).willReturn(aResponse().withStatus(404)));

    String url = wireMockServer.baseUrl() + endpoint;

    assertThrows(NotFoundException.class, () -> getBody(url));
  }

  @Test
  void testRestClient400HandlerIsInvokedBeforeGenericErrorHandler() {
    String endpoint = "/bad-request";
    wireMockServer.stubFor(
        get(urlEqualTo(endpoint))
            .willReturn(aResponse().withStatus(400).withBody("Bad request error")));

    String url = wireMockServer.baseUrl() + endpoint;

    assertThrows(BadRequestException.class, () -> getBody(url));
  }

  private String getBody(String url) {
    return restClient.get().uri(url).retrieve().body(String.class);
  }
}
