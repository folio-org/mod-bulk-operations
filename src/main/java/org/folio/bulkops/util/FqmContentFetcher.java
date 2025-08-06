package org.folio.bulkops.util;

import static java.lang.String.format;
import static java.lang.String.join;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.SPACE;
import static org.folio.bulkops.util.Constants.ENTITY;
import static org.folio.bulkops.util.Constants.HOLDINGS_DATA;
import static org.folio.bulkops.util.Constants.HOLDINGS_LOCATION_CALL_NUMBER_DELIMITER;
import static org.folio.bulkops.util.Constants.ID;
import static org.folio.bulkops.util.Constants.INSTANCE_TITLE;
import static org.folio.bulkops.util.Constants.LINE_BREAK;
import static org.folio.bulkops.util.Constants.TENANT_ID;
import static org.folio.bulkops.util.Constants.TITLE;
import static org.folio.bulkops.util.FqmKeys.FQM_DATE_OF_PUBLICATION_KEY;
import static org.folio.bulkops.util.FqmKeys.FQM_HOLDINGS_RECORD_INSTANCE_PUBLICATION;
import static org.folio.bulkops.util.FqmKeys.FQM_INSTANCES_TITLE_KEY;
import static org.folio.bulkops.util.FqmKeys.FQM_INSTANCE_TITLE_KEY;
import static org.folio.bulkops.util.FqmKeys.FQM_ITEM_INSTANCES_PUBLICATION_KEY;
import static org.folio.bulkops.util.FqmKeys.FQM_PUBLISHER_KEY;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.annotation.PostConstruct;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.folio.bulkops.client.QueryClient;
import org.folio.bulkops.domain.bean.StateType;
import org.folio.bulkops.domain.dto.EntityType;
import org.folio.bulkops.domain.dto.ErrorType;
import org.folio.bulkops.domain.entity.BulkOperationExecutionContent;
import org.folio.bulkops.exception.FqmFetcherException;
import org.folio.bulkops.service.ConsortiaService;
import org.folio.spring.FolioExecutionContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;


@Service
@Log4j2
@RequiredArgsConstructor
public class FqmContentFetcher {

  public static final String TITLE_PATTERN = "%s. %s, %s";
  @Value("${application.fqm-fetcher.max_parallel_chunks}")
  private int maxParallelChunks;
  @Value("${application.fqm-fetcher.max_chunk_size}")
  private int chunkSize;

  private final QueryClient queryClient;
  private final ObjectMapper objectMapper;
  private final FolioExecutionContext folioExecutionContext;
  private final ConsortiaService consortiaService;

  @PostConstruct
  private void logStartup() {
    log.info("FqmContentFetcher initialized with parameters - max_parallel_chunks: {}, max_chunk_size: {}", maxParallelChunks, chunkSize);
  }

  /**
   * Fetches content in parallel using multiple threads.
   *
   * @param queryId the FQM query ID
   * @param entityType the type of entity to fetch
   * @param total the total number of records to fetch
   * @return an InputStream containing the fetched content
   */
  public InputStream fetch(UUID queryId, EntityType entityType, int total, List<BulkOperationExecutionContent> bulkOperationExecutionContents,
                           UUID operationId) {
    try (var executor = Executors.newFixedThreadPool(maxParallelChunks)) {
      int chunks = (total + chunkSize - 1) / chunkSize;

      List<CompletableFuture<InputStream>> tasks = IntStream.range(0, chunks)
          .mapToObj(chunk ->
              CompletableFuture.supplyAsync(() -> task(queryId, entityType, chunk, total, bulkOperationExecutionContents, operationId), executor)
          )
          .toList();

      CompletableFuture<List<InputStream>> future = CompletableFuture
          .allOf(tasks.toArray(new CompletableFuture[0]))
          .thenApply(ignored ->
              tasks.stream()
                  .map(CompletableFuture::join)
                  .toList()
          );

      var futures = future.join();

      return new SequenceInputStream(Collections.enumeration(futures));
    } catch (Exception e) {
      log.error("Error fetching content", e);
      throw new FqmFetcherException("Error fetching content", e);
    }
  }

  private InputStream task(UUID queryId, EntityType entityType, int chunk, int total, List<BulkOperationExecutionContent> bulkOperationExecutionContents,
                           UUID operationId) {
    int offset = chunk * chunkSize;
    int limit = Math.min(chunkSize, total - offset);
    var response = queryClient.getQuery(queryId, offset, limit).getContent();
    return new ByteArrayInputStream(response.stream()
        .map(json -> {
          try {
            var jsonb = json.get(getEntityJsonKey(entityType));
            if (entityType == EntityType.USER) {
              return jsonb.toString();
            }
            var jsonNode = (ObjectNode) objectMapper.readTree(jsonb.toString());
            var tenant = json.get(getContentTenantKey(entityType));
            if (tenant == null) {
              checkForTenantFieldExistenceInEcs(jsonNode.get(ID).asText(), operationId, bulkOperationExecutionContents);
              tenant = folioExecutionContext.getTenantId();
            }
            jsonNode.put(TENANT_ID, tenant.toString());

            if (entityType == EntityType.ITEM) {

              var title = ofNullable(json.get(FQM_INSTANCES_TITLE_KEY)).orElse(EMPTY).toString();
              var publications = objectMapper.readTree(ofNullable(json.get(FQM_ITEM_INSTANCES_PUBLICATION_KEY)).orElse("[]").toString());

              var publisher = EMPTY;
              var date = EMPTY;

              if (publications.isArray() && !publications.isEmpty()) {
                var first = publications.get(0);
                if (nonNull(first)) {
                  publisher = first.get(FQM_PUBLISHER_KEY).asText(EMPTY);
                  date = first.get(FQM_DATE_OF_PUBLICATION_KEY).asText(EMPTY);
                }
              }

              jsonNode.put(TITLE,  format(TITLE_PATTERN, title, publisher, date));

              var callNumber = Stream.of(json.get(FqmKeys.FQM_HOLDINGS_CALL_NUMBER_PREFIX_KEY), json.get(
                      FqmKeys.FQM_HOLDINGS_CALL_NUMBER_KEY), json.get(
                      FqmKeys.FQM_HOLDINGS_CALL_NUMBER_SUFFIX_KEY))
                  .filter(Objects::nonNull)
                  .map(Object::toString)
                  .collect(Collectors.joining(SPACE));

              var permanentLocationName = ofNullable(json.get(
                  FqmKeys.FQM_PERMANENT_LOCATION_NAME_KEY)).orElse(EMPTY).toString();

              jsonNode.put(HOLDINGS_DATA,
                  join(HOLDINGS_LOCATION_CALL_NUMBER_DELIMITER,
                      permanentLocationName,
                      callNumber));
            }

            if (entityType == EntityType.HOLDINGS_RECORD) {

              var title = ofNullable(json.get(FQM_INSTANCE_TITLE_KEY)).orElse(EMPTY).toString();
              var publications = objectMapper.readTree(ofNullable(json.get(FQM_HOLDINGS_RECORD_INSTANCE_PUBLICATION)).orElse("[]").toString());

              var publisher = EMPTY;
              var date = EMPTY;

              if (publications.isArray() && !publications.isEmpty()) {
                var first = publications.get(0);
                if (nonNull(first)) {
                  publisher = first.get(FQM_PUBLISHER_KEY).asText(EMPTY);
                  date = first.get(FQM_DATE_OF_PUBLICATION_KEY).asText(EMPTY);
                }
              }

              jsonNode.put(INSTANCE_TITLE, format(TITLE_PATTERN, title, publisher, date));
            }

            ObjectNode extendedRecordWrapper = objectMapper.createObjectNode();
            extendedRecordWrapper.set(ENTITY, jsonNode);
            extendedRecordWrapper.put(TENANT_ID, tenant.toString());
            return objectMapper.writeValueAsString(extendedRecordWrapper);
          } catch (Exception e) {
            log.error("Error processing JSON content for entity type", e);
            return EMPTY;
          }
        })
        .collect(Collectors.joining(LINE_BREAK))
        .getBytes(StandardCharsets.UTF_8));
  }

  private String getEntityJsonKey(EntityType entityType) {
    return switch(entityType) {
      case USER -> FqmKeys.FQM_USERS_JSONB_KEY;
      case ITEM -> FqmKeys.FQM_ITEMS_JSONB_KEY;
      case HOLDINGS_RECORD -> "holdings.jsonb";
      case INSTANCE, INSTANCE_MARC -> "instance.jsonb";
    };
  }

  private String getContentTenantKey(EntityType entityType) {
    return switch(entityType) {
      case USER -> StringUtils.EMPTY;
      case ITEM -> "items.tenant_id";
      case HOLDINGS_RECORD -> "holdings.tenant_id";
      case INSTANCE, INSTANCE_MARC -> "instance.tenant_id";
    };
  }

  private void checkForTenantFieldExistenceInEcs(String recordId, UUID operationId, List<BulkOperationExecutionContent> bulkOperationExecutionContents) {
    if (consortiaService.isTenantCentral(folioExecutionContext.getTenantId())) {
      var errorMessage = "Cannot get reference data: tenant field is missing in the FQM response for ECS environment.";
      log.warn(errorMessage);
      bulkOperationExecutionContents.add(BulkOperationExecutionContent.builder()
              .identifier(recordId)
              .bulkOperationId(operationId)
              .state(StateType.PROCESSED)
              .errorType(ErrorType.WARNING)
              .errorMessage(errorMessage)
              .build());
    }
  }
}
