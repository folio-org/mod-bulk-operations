package org.folio.bulkops.util;

import jakarta.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.bulkops.client.QueryClient;
import org.folio.bulkops.domain.bean.fqm.ContentRequest;
import org.folio.bulkops.domain.dto.EntityType;
import org.folio.bulkops.exception.FqmFetcherException;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;


@Service
@Log4j2
@RequiredArgsConstructor
public class IdsContentFetcher {

  @Value("${application.fqm-fetcher.max_parallel_chunks}")
  private int maxParallelChunks;
  @Value("${application.fqm-fetcher.max_chunk_size}")
  private int chunkSize;

  private final QueryClient queryClient;

  @PostConstruct
  private void logStartUp() {
    log.info("FqmContentFetcher initialized with parameters - max_parallel_chunks: {}, max_chunk_size: {}", maxParallelChunks, chunkSize);
  }


  /**
   * Fetches content in parallel using FQM client (sync mode) based on a list of UUIDs.
   * @param uuids the list of UUIDs to fetch content
   * @param entityType the type of entity to fetch
   * @return an InputStream containing the fetched content
   */
  public InputStream fetch(List<UUID> uuids, EntityType entityType) {
    try (var executor = Executors.newFixedThreadPool(maxParallelChunks)) {

      var tasks = executeParallelTasks(uuids, entityType, executor);
      var streams = joinParallelTasks(tasks);

      return new SequenceInputStream(Collections.enumeration(streams));
    } catch (Exception e) {
      log.error("Error fetching content", e);
      throw new FqmFetcherException("Error fetching content", e);
    }
  }

  private @NotNull List<CompletableFuture<InputStream>> executeParallelTasks(List<UUID> uuids,
      EntityType entityType, ExecutorService executor) {
    return IntStream.range(0, (uuids.size() + chunkSize - 1) / chunkSize)
        .mapToObj(i -> uuids.subList(i * chunkSize, Math.min(uuids.size(), (i + 1) * chunkSize)))
        .map(chunk ->
            CompletableFuture.supplyAsync(() -> task(chunk, entityType), executor)
        )
        .toList();
  }

  private List<InputStream> joinParallelTasks(List<CompletableFuture<InputStream>> tasks) {
    return CompletableFuture
        .allOf(tasks.toArray(new CompletableFuture[0]))
        .thenApply(vVoid ->
            tasks.stream()
                .map(CompletableFuture::join)
                .toList())
        .join();
  }

  private InputStream task(List<UUID> chunk, EntityType entityType) {

    var content = queryClient.getContents(ContentRequest.builder()
        .ids(chunk)
        .fields(List.of(getContentJsonKey(entityType)))
        .localize(false)
        .entityTypeId(UUID.fromString("6b08439b-4f8e-4468-8046-ea620f5cfb74")).build());

    return new ByteArrayInputStream(content.stream()
        .map(json -> json.get(getContentJsonKey(entityType)).toString()).collect(Collectors.joining("\n"))
        .getBytes(StandardCharsets.UTF_8));
  }

  private String getContentJsonKey(EntityType entityType) {
    return switch(entityType) {
      case USER -> "users.jsonb";
      case ITEM -> "items.jsonb";
      case HOLDINGS_RECORD -> "holdings.jsonb";
      case INSTANCE, INSTANCE_MARC -> "instance.jsonb";
    };
  }
}
