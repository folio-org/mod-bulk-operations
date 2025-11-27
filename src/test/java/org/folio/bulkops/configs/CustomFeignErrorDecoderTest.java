package org.folio.bulkops.configs;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import feign.Request;
import feign.RequestTemplate;
import feign.Response;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.UUID;
import org.folio.bulkops.exception.BadRequestException;
import org.folio.bulkops.exception.BulkEditException;
import org.folio.bulkops.exception.NotFoundException;
import org.junit.jupiter.api.Test;

class CustomFeignErrorDecoderTest {

  private final CustomFeignErrorDecoder customFeignErrorDecoder = new CustomFeignErrorDecoder();

  @Test
  void shouldReturnNotFoundExceptionWhenStatus404() {
    Response response =
        Response.builder()
            .request(
                Request.create(
                    Request.HttpMethod.GET,
                    "not found url",
                    Map.of(),
                    Request.Body.create("Error at index "),
                    new RequestTemplate()))
            .status(404)
            .build();
    var actual = customFeignErrorDecoder.decode("", response);

    assertInstanceOf(NotFoundException.class, actual);
  }

  @Test
  void shouldReturnBadRequestExceptionWhenStatus400() {
    Response response =
        Response.builder()
            .request(
                Request.create(
                    Request.HttpMethod.GET,
                    "not found url: /" + UUID.randomUUID(),
                    Map.of(),
                    Request.Body.create("Error at index "),
                    new RequestTemplate()))
            .body("Error at index", Charset.defaultCharset())
            .status(400)
            .build();
    var actual = customFeignErrorDecoder.decode("", response);

    assertInstanceOf(BadRequestException.class, actual);
  }

  @Test
  void shouldReturnInternalServerErrorExceptionWhenStatus500() {
    Response response =
        Response.builder()
            .request(
                Request.create(
                    Request.HttpMethod.GET,
                    "internal server error url",
                    Map.of(),
                    Request.Body.create(""),
                    new RequestTemplate()))
            .status(500)
            .build();
    var actual = customFeignErrorDecoder.decode("", response);

    assertInstanceOf(BulkEditException.class, actual);
  }

  @Test
  void shouldReturnBadRequestExceptionWhenStatus400AndNoBody() {
    Response response =
        Response.builder()
            .request(
                Request.create(
                    Request.HttpMethod.GET,
                    "bad request url",
                    Map.of(),
                    Request.Body.create(""),
                    new RequestTemplate()))
            .status(400)
            .body("bad request".getBytes())
            .build();
    var actual = customFeignErrorDecoder.decode("", response);

    assertInstanceOf(BadRequestException.class, actual);
  }

  @Test
  void shouldReturnBadRequestExceptionWhenIoExceptionOccurs() {
    Response response =
        Response.builder()
            .request(
                Request.create(
                    Request.HttpMethod.GET,
                    "ioexception url",
                    Map.of(),
                    Request.Body.create(""),
                    new RequestTemplate()))
            .body(getResponseBody())
            .status(400)
            .build();

    var actual = customFeignErrorDecoder.decode("", response);

    assertInstanceOf(BadRequestException.class, actual);
  }

  @Test
  void shouldReturnFeignExceptionForUnhandledStatus() {
    Response response =
        Response.builder()
            .request(
                Request.create(
                    Request.HttpMethod.GET,
                    "some url",
                    Map.of(),
                    Request.Body.create("Unhandled error"),
                    new RequestTemplate()))
            .status(418)
            .reason("I'm a teapot")
            .build();

    Exception actual = customFeignErrorDecoder.decode("methodKey", response);

    assertInstanceOf(feign.FeignException.class, actual);
  }

  @Test
  void shouldReturnBulkEditExceptionWithUnknownErrorWhenReasonIsNull() {
    Response response =
        Response.builder()
            .request(
                Request.create(
                    Request.HttpMethod.GET,
                    "internal server error url",
                    Map.of(),
                    Request.Body.create(""),
                    new RequestTemplate()))
            .status(500)
            .reason(null)
            .build();

    Exception actual = customFeignErrorDecoder.decode("", response);
    assertInstanceOf(BulkEditException.class, actual);
    // Optionally, check the message contains "Unknown error"
    assert (actual.getMessage().contains("Unknown error"));
  }

  @Test
  void shouldReturnBulkEditExceptionWithReasonWhenReasonIsNotNull() {
    String reason = "Internal server error occurred";
    Response response =
        Response.builder()
            .request(
                Request.create(
                    Request.HttpMethod.GET,
                    "internal server error url",
                    Map.of(),
                    Request.Body.create(""),
                    new RequestTemplate()))
            .status(500)
            .reason(reason)
            .build();

    Exception actual = customFeignErrorDecoder.decode("", response);
    assertInstanceOf(BulkEditException.class, actual);
    assert (actual.getMessage().contains(reason));
  }

  private feign.Response.Body getResponseBody() {
    return new feign.Response.Body() {
      @Override
      public java.io.InputStream asInputStream() throws IOException {
        throw new IOException("Simulated IO error");
      }

      @Override
      public Reader asReader(Charset charset) throws IOException {
        throw new IOException("Simulated IO error");
      }

      @Override
      public void close() {
        // No-op for closing resources
      }

      @Override
      public Integer length() {
        return null;
      }

      @Override
      public boolean isRepeatable() {
        return false;
      }
    };
  }
}
