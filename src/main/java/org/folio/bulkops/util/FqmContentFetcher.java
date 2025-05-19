package org.folio.bulkops.util;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.bulkops.client.QueryClient;
import org.folio.bulkops.exception.FqmFetcherException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;


@Service
@Log4j2
@RequiredArgsConstructor
public class FqmContentFetcher {

  @Value("${application.fqm-fetcher.max_parallel_chunks}")
  private int maxParallelChunks;
  @Value("${application.fqm-fetcher.max_chunk_size}")
  private int chunkSize;
  @Autowired
  private QueryClient queryClient;

  /**
   * Fetches content in parallel using multiple threads.
   *
   * @param queryId the FQM query ID
   * @param total the total number of records to fetch
   * @return an InputStream containing the fetched content
   */
  public InputStream fetch(UUID queryId, int total) {
    try (var executor = Executors.newFixedThreadPool(maxParallelChunks)) {
      int chunks = (total + chunkSize - 1) / chunkSize;

      List<CompletableFuture<InputStream>> tasks = IntStream.range(0, chunks)
          .mapToObj(chunk ->
              CompletableFuture.supplyAsync(() -> task(queryId, chunk, total), executor)
          )
          .toList();

      CompletableFuture<List<InputStream>> future = CompletableFuture
          .allOf(tasks.toArray(new CompletableFuture[0]))
          .thenApply(ignored ->
              tasks.stream()
                  .map(CompletableFuture::join)
                  .toList()
          );

      try {
        return new SequenceInputStream(Collections.enumeration(future.join()));
      } catch (CompletionException ex) {
        throw new FqmFetcherException("Failed to fetch one or more chunks", ex);
      }
    }
  }

  private InputStream task(UUID queryId, int chunk, int total) {
    int offset = chunk * chunkSize;
    int limit = Math.min(chunkSize, total - offset);
    var response = queryClient.getQuery(queryId, offset, limit).getContent();
    return new ByteArrayInputStream(response.stream()
        .map(json -> json.get("instance.jsonb").toString()).collect(Collectors.joining(",\n"))
        .getBytes(StandardCharsets.UTF_8));
  }
}
