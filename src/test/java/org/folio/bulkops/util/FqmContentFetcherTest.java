package org.folio.bulkops.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.stream.IntStream;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import feign.Request;
import feign.Response;
import org.folio.bulkops.BaseTest;
import org.folio.bulkops.client.QueryClient;
import org.folio.bulkops.domain.dto.EntityType;
import org.folio.bulkops.exception.FqmFetcherException;
import org.folio.querytool.domain.dto.QueryDetails;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@ContextConfiguration(initializers = BaseTest.Initializer.class)
class FqmContentFetcherTest {

  @Autowired
  private FqmContentFetcher fqmContentFetcher;

  @Value("${application.fqm-fetcher.max_chunk_size}")
  private int chunkSize;

  @MockitoBean
  private QueryClient queryClient;

  @Test
  void fetchShouldProcessMultipleChunksInParallel() throws IOException {

    var queryId = UUID.randomUUID();
    var data = getMockedData(0, Integer.MAX_VALUE);
    var total = data.getContent().size();
    var expected = data.getContent().stream().map(json -> json.get("instance.jsonb").toString()).toList();

    IntStream.range(0, (total + chunkSize - 1) / chunkSize).forEach(chunk -> {
      int offset = chunk * chunkSize;
      int limit = Math.min(chunkSize, total - offset);
      when(queryClient.getQuery(queryId, offset, limit)).thenReturn(getMockedData(offset, limit));
    });

    try (var is = fqmContentFetcher.fetch(queryId, EntityType.INSTANCE, total)) {
      var actual = new String(is.readAllBytes(), StandardCharsets.UTF_8);
      assertThat(actual).contains(expected);
    }
  }

  @Test
  void fetchShouldFailMultipleChunksInParallel() {

    var queryId = UUID.randomUUID();
    var data = getMockedData(0, Integer.MAX_VALUE);
    var total = data.getContent().size();

    IntStream.range(0, ((total + chunkSize - 1) / chunkSize) - 1).forEach(chunk -> {
      int offset = chunk * chunkSize;
      int limit = Math.min(chunkSize, total - offset);
      when(queryClient.getQuery(queryId, offset, limit)).thenReturn(getMockedData(offset, limit));
    });

    int offset =  ((total + chunkSize - 1) / chunkSize - 1) * chunkSize;
    int limit = Math.min(chunkSize, total - offset);

    doThrow(FeignException.errorStatus("", Response.builder().status(500)
        .reason("GET-Error")
        .request(Request.create(Request.HttpMethod.GET, "", Map.of(), new byte[]{}, Charset.defaultCharset(), null))
        .build())).when(queryClient).getQuery(queryId, offset, limit);

    Exception exception = assertThrows(FqmFetcherException.class, () -> fqmContentFetcher.fetch(queryId, EntityType.INSTANCE, total));

    assertThat(exception.getCause().getCause()).isInstanceOf(FeignException.class);
  }

  private QueryDetails getMockedData(int offset, int limit) {

    try {
      var mapper = new ObjectMapper();
      var is = getClass().getClassLoader().getResourceAsStream("fqmClient/fqmClientResponse.json");
      var data = mapper.readValue(is, new TypeReference<QueryDetails>() {
      });

      var content = data.getContent().stream()
          .skip(offset)
          .limit(limit)
          .toList();

      return data.content(content);
    } catch (Exception e) {
      throw new RuntimeException("Failed to load FQM response data for testing", e);
    }
  }
}
