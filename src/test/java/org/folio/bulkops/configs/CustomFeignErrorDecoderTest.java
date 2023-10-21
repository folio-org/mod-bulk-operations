package org.folio.bulkops.configs;

import feign.Request;
import feign.RequestTemplate;
import feign.Response;
import org.folio.bulkops.BaseTest;
import org.folio.bulkops.exception.NotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.when;

public class CustomFeignErrorDecoderTest extends BaseTest {

  @MockBean
  private CustomFeignErrorDecoder errorDecoder;

  @Test
  void shouldReturnNotFoundExceptionWhenStatusNotFound() {

    Response response = Response.builder()
      .request(Request.create(Request.HttpMethod.GET, "not found url", Map.of(), Request.Body.create(""),
        new RequestTemplate())).status(404).build();

    when(errorDecoder.decode("", response)).thenReturn(new NotFoundException("Not found: not found url"));

    var actual = errorDecoder.decode("", response);
    assertInstanceOf(NotFoundException.class, actual);
  }
}
