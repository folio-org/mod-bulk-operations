package org.folio.bulkops.configs;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.nio.charset.Charset;
import java.util.Map;
import java.util.UUID;

import feign.Request;
import feign.RequestTemplate;
import feign.Response;
import org.folio.bulkops.exception.BadRequestException;
import org.folio.bulkops.exception.BulkEditException;
import org.folio.bulkops.exception.NotFoundException;
import org.junit.jupiter.api.Test;

class CustomFeignErrorDecoderTest {

  private final CustomFeignErrorDecoder customFeignErrorDecoder = new CustomFeignErrorDecoder();

  @Test
  void shouldReturnNotFoundExceptionWhenStatus404() {
    Response response = Response.builder()
      .request(Request.create(Request.HttpMethod.GET, "not found url", Map.of(),
        Request.Body.create("Error at index "), new RequestTemplate())).status(404).build();
    var actual = customFeignErrorDecoder.decode("", response);

    assertInstanceOf(NotFoundException.class, actual);
  }

  @Test
  void shouldReturnBadRequestExceptionWhenStatus400() {
    Response response = Response.builder()
      .request(Request.create(Request.HttpMethod.GET, "not found url: /" + UUID.randomUUID(), Map.of(),
        Request.Body.create("Error at index "), new RequestTemplate())).body("Error at index",
        Charset.defaultCharset()).status(400).build();
    var actual = customFeignErrorDecoder.decode("", response);

    assertInstanceOf(BadRequestException.class, actual);
  }

  @Test
  void shouldReturnInternalServerErrorExceptionWhenStatus500() {
    Response response = Response.builder()
      .request(Request.create(Request.HttpMethod.GET, "internal server error url", Map.of(),
        Request.Body.create(""), new RequestTemplate())).status(500).build();
    var actual = customFeignErrorDecoder.decode("", response);

    assertInstanceOf(BulkEditException.class, actual);
  }

  @Test
  void shouldReturnBadRequestExceptionWhenStatus500() {
    Response response = Response.builder()
            .request(Request.create(Request.HttpMethod.GET, "bad request url", Map.of(),
                    Request.Body.create(""), new RequestTemplate())).status(400)
            .body("bad request".getBytes()).build();
    var actual = customFeignErrorDecoder.decode("", response);

    assertInstanceOf(BadRequestException.class, actual);
  }
}
