package org.folio.bulkops.util;

import static java.lang.String.join;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.SPACE;
import static org.folio.bulkops.util.Constants.CHILD_INSTANCES;
import static org.folio.bulkops.util.Constants.ENTITY;
import static org.folio.bulkops.util.Constants.HOLDINGS_DATA;
import static org.folio.bulkops.util.Constants.HOLDINGS_LOCATION_CALL_NUMBER_DELIMITER;
import static org.folio.bulkops.util.Constants.ID;
import static org.folio.bulkops.util.Constants.INSTANCE_TITLE;
import static org.folio.bulkops.util.Constants.LINE_BREAK;
import static org.folio.bulkops.util.Constants.MSG_SHADOW_RECORDS_CANNOT_BE_EDITED;
import static org.folio.bulkops.util.Constants.NO_MATCH_FOUND_MESSAGE;
import static org.folio.bulkops.util.Constants.PARENT_INSTANCES;
import static org.folio.bulkops.util.Constants.PRECEDING_TITLES;
import static org.folio.bulkops.util.Constants.SUCCEEDING_TITLES;
import static org.folio.bulkops.util.Constants.TENANT_ID;
import static org.folio.bulkops.util.Constants.TITLE;
import static org.folio.bulkops.util.FqmKeys.FQM_DATE_OF_PUBLICATION_KEY;
import static org.folio.bulkops.util.FqmKeys.FQM_INSTANCES_PUBLICATION_KEY;
import static org.folio.bulkops.util.FqmKeys.FQM_INSTANCES_TITLE_KEY;
import static org.folio.bulkops.util.FqmKeys.FQM_INSTANCES_WITH_HOLDINGS_KEY;
import static org.folio.bulkops.util.FqmKeys.FQM_INSTANCE_CHILD_INSTANCES_KEY;
import static org.folio.bulkops.util.FqmKeys.FQM_INSTANCE_ID_KEY;
import static org.folio.bulkops.util.FqmKeys.FQM_INSTANCE_PARENT_INSTANCES_KEY;
import static org.folio.bulkops.util.FqmKeys.FQM_INSTANCE_PRECEDING_TITLES_KEY;
import static org.folio.bulkops.util.FqmKeys.FQM_INSTANCE_PUBLICATION_KEY;
import static org.folio.bulkops.util.FqmKeys.FQM_INSTANCE_SHARED_KEY;
import static org.folio.bulkops.util.FqmKeys.FQM_INSTANCE_SUCCEEDING_TITLES_KEY;
import static org.folio.bulkops.util.FqmKeys.FQM_INSTANCE_TITLE_KEY;
import static org.folio.bulkops.util.FqmKeys.FQM_PUBLISHER_KEY;
import static org.folio.bulkops.util.FqmKeys.FQM_USERS_ID_KEY;
import static org.folio.bulkops.util.FqmKeys.FQM_USERS_TYPE_KEY;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
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

  public static final String SHARED = "Shared";
  public static final String SHADOW = "shadow";
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
    log.info(
        "FqmContentFetcher initialized with parameters - max_parallel_chunks: {}, "
            + "max_chunk_size: {}",
        maxParallelChunks,
        chunkSize);
  }

  /**
   * Fetches content in parallel using multiple threads.
   *
   * @param queryId the FQM query ID
   * @param entityType the type of entity to fetch
   * @param total the total number of records to fetch
   * @return an InputStream containing the fetched content
   */
  public InputStream fetch(
      UUID queryId,
      EntityType entityType,
      int total,
      List<BulkOperationExecutionContent> bulkOperationExecutionContents,
      UUID operationId) {
    try (var executor = Executors.newFixedThreadPool(maxParallelChunks)) {
      int chunks = (total + chunkSize - 1) / chunkSize;

      boolean isCentralTenant =
          consortiaService.isTenantCentral(folioExecutionContext.getTenantId());

      List<CompletableFuture<InputStream>> tasks =
          IntStream.range(0, chunks)
              .mapToObj(
                  chunk ->
                      CompletableFuture.supplyAsync(
                          () ->
                              task(
                                  queryId,
                                  entityType,
                                  chunk,
                                  total,
                                  bulkOperationExecutionContents,
                                  operationId,
                                  isCentralTenant),
                          executor))
              .toList();

      CompletableFuture<List<InputStream>> future =
          CompletableFuture.allOf(tasks.toArray(new CompletableFuture[0]))
              .thenApply(ignored -> tasks.stream().map(CompletableFuture::join).toList());

      var futures = future.join();

      return new SequenceInputStream(Collections.enumeration(futures));
    } catch (Exception e) {
      log.error("Error fetching content", e);
      throw new FqmFetcherException("Error fetching content", e);
    }
  }

  private InputStream task(
      UUID queryId,
      EntityType entityType,
      int chunk,
      int total,
      List<BulkOperationExecutionContent> bulkOperationExecutionContents,
      UUID operationId,
      boolean isCentralTenant) {
    int offset = chunk * chunkSize;
    int limit = Math.min(chunkSize, total - offset);
    var response = queryClient.getQuery(queryId, offset, limit).getContent();
    return new ByteArrayInputStream(
        response.stream()
            .map(
                json -> {
                  try {
                    if (isCentralTenant) {
                      if (isInstance(entityType)
                          && !SHARED.equalsIgnoreCase(
                              ofNullable(json.get(FQM_INSTANCE_SHARED_KEY))
                                  .map(Object::toString)
                                  .orElse(EMPTY))) {
                        addInstanceNoMatchFound(json, bulkOperationExecutionContents, operationId);
                        return EMPTY;
                      } else if (EntityType.USER.equals(entityType)
                          && SHADOW.equalsIgnoreCase(
                              ofNullable(json.get(FQM_USERS_TYPE_KEY))
                                  .map(Object::toString)
                                  .orElse(EMPTY))) {
                        addShadowUserErrorContent(
                            json, bulkOperationExecutionContents, operationId);
                        return EMPTY;
                      }
                    }

                    var jsonb = json.get(getEntityJsonKey(entityType));
                    if (entityType == EntityType.USER) {
                      return jsonb.toString();
                    }
                    var jsonNode = (ObjectNode) objectMapper.readTree(jsonb.toString());
                    if (sharedMarcFromMemberHasNoHoldings(
                        json, entityType, jsonNode)) {
                      addInstanceNoMatchFound(json, bulkOperationExecutionContents, operationId);
                      return EMPTY;
                    }
                    var tenant = json.get(getContentTenantKey(entityType));
                    if (tenant == null) {
                      checkForTenantFieldExistenceInEcs(
                          jsonNode.get(ID).asText(), operationId, bulkOperationExecutionContents);
                      tenant = folioExecutionContext.getTenantId();
                    }
                    jsonNode.put(TENANT_ID, tenant.toString());

                    if (entityType == EntityType.ITEM) {
                      processForItem(json, jsonNode);
                    }

                    if (entityType == EntityType.HOLDINGS_RECORD) {
                      processForHoldingsRecord(json, jsonNode);
                    }

                    if (isInstance(entityType)) {
                      processInstanceEntity(json, jsonNode);
                    }

                    ObjectNode extendedRecordWrapper = objectMapper.createObjectNode();
                    extendedRecordWrapper.set(ENTITY, jsonNode);
                    extendedRecordWrapper.put(TENANT_ID, tenant.toString());
                    return objectMapper.writeValueAsString(extendedRecordWrapper);
                  } catch (Exception e) {
                    log.error("Error processing JSON content: {}", json, e);
                    return EMPTY;
                  }
                })
            .filter(StringUtils::isNotEmpty)
            .collect(Collectors.joining(LINE_BREAK))
            .getBytes(StandardCharsets.UTF_8));
  }

  private String getEntityJsonKey(EntityType entityType) {
    return switch (entityType) {
      case USER -> FqmKeys.FQM_USERS_JSONB_KEY;
      case ITEM -> FqmKeys.FQM_ITEMS_JSONB_KEY;
      case HOLDINGS_RECORD -> "holdings.jsonb";
      case INSTANCE, INSTANCE_MARC -> "instance.jsonb";
    };
  }

  private String getContentTenantKey(EntityType entityType) {
    return switch (entityType) {
      case USER -> StringUtils.EMPTY;
      case ITEM -> "items.tenant_id";
      case HOLDINGS_RECORD -> "holdings.tenant_id";
      case INSTANCE, INSTANCE_MARC -> "instance.tenant_id";
    };
  }

  private void checkForTenantFieldExistenceInEcs(
      String recordId,
      UUID operationId,
      List<BulkOperationExecutionContent> bulkOperationExecutionContents) {
    if (consortiaService.isTenantCentral(folioExecutionContext.getTenantId())) {
      var errorMessage =
          "Cannot get reference data: tenant field is missing in "
              + "the FQM response for ECS environment.";
      log.warn(errorMessage);
      bulkOperationExecutionContents.add(
          BulkOperationExecutionContent.builder()
              .identifier(recordId)
              .bulkOperationId(operationId)
              .state(StateType.PROCESSED)
              .errorType(ErrorType.WARNING)
              .errorMessage(errorMessage)
              .build());
    }
  }

  private String getFirstPublicationAsString(JsonNode publication) {
    if (nonNull(publication)) {
      var publisher = publication.get(FQM_PUBLISHER_KEY);
      var date = publication.get(FQM_DATE_OF_PUBLICATION_KEY);
      if (isNull(date) || date.isNull()) {
        return isNull(publisher) || publisher.isNull()
            ? EMPTY
            : String.format(". %s", publisher.asText());
      }
      return String.format(
          ". %s, %s",
          isNull(publisher) || publisher.isNull() ? EMPTY : publisher.asText(), date.asText());
    }
    return EMPTY;
  }

  private boolean isInstance(EntityType entityType) {
    return entityType == EntityType.INSTANCE || entityType == EntityType.INSTANCE_MARC;
  }

  private void addInstanceNoMatchFound(
      Map<String, Object> json,
      List<BulkOperationExecutionContent> bulkOperationExecutionContents,
      UUID operationId) {
    var instanceId = ofNullable(json.get(FQM_INSTANCE_ID_KEY)).map(Object::toString).orElse(EMPTY);
    bulkOperationExecutionContents.add(
        BulkOperationExecutionContent.builder()
            .identifier(instanceId)
            .bulkOperationId(operationId)
            .state(StateType.FAILED)
            .errorType(ErrorType.ERROR)
            .errorMessage(NO_MATCH_FOUND_MESSAGE)
            .build());
  }

  private void addShadowUserErrorContent(
      Map<String, Object> json,
      List<BulkOperationExecutionContent> bulkOperationExecutionContents,
      UUID operationId) {
    var userId = ofNullable(json.get(FQM_USERS_ID_KEY)).map(Object::toString).orElse(EMPTY);
    bulkOperationExecutionContents.add(
        BulkOperationExecutionContent.builder()
            .identifier(userId)
            .bulkOperationId(operationId)
            .state(StateType.FAILED)
            .errorType(ErrorType.ERROR)
            .errorMessage(MSG_SHADOW_RECORDS_CANNOT_BE_EDITED)
            .build());
  }

  private void processForItem(Map<String, Object> json, ObjectNode jsonNode)
      throws JsonProcessingException {
    var title = ofNullable(json.get(FQM_INSTANCES_TITLE_KEY)).orElse(EMPTY).toString();
    var value = json.get(FQM_INSTANCES_PUBLICATION_KEY);

    var publications =
        nonNull(value) ? objectMapper.readTree(value.toString()) : objectMapper.createArrayNode();

    if (publications.isArray() && !publications.isEmpty()) {
      title = title.concat(getFirstPublicationAsString(publications.get(0)));
    }

    jsonNode.put(TITLE, title);

    var callNumber =
        Stream.of(
                json.get(FqmKeys.FQM_HOLDINGS_CALL_NUMBER_PREFIX_KEY),
                json.get(FqmKeys.FQM_HOLDINGS_CALL_NUMBER_KEY),
                json.get(FqmKeys.FQM_HOLDINGS_CALL_NUMBER_SUFFIX_KEY))
            .filter(Objects::nonNull)
            .map(Object::toString)
            .collect(Collectors.joining(SPACE));

    var permanentLocationName =
        ofNullable(json.get(FqmKeys.FQM_HOLDING_PERMANENT_LOCATION_NAME_KEY))
            .orElse(EMPTY)
            .toString();

    jsonNode.put(
        HOLDINGS_DATA,
        join(HOLDINGS_LOCATION_CALL_NUMBER_DELIMITER, permanentLocationName, callNumber));
  }

  private void processForHoldingsRecord(Map<String, Object> json, ObjectNode jsonNode)
      throws JsonProcessingException {
    var title = ofNullable(json.get(FQM_INSTANCE_TITLE_KEY)).orElse(EMPTY).toString();

    var value = json.get(FQM_INSTANCE_PUBLICATION_KEY);

    var publications =
        nonNull(value) ? objectMapper.readTree(value.toString()) : objectMapper.createArrayNode();

    if (publications.isArray() && !publications.isEmpty()) {
      title = title.concat(getFirstPublicationAsString(publications.get(0)));
    }

    jsonNode.put(INSTANCE_TITLE, title);
  }

  private void processInstanceEntity(Map<String, Object> json, ObjectNode jsonNode)
      throws JsonProcessingException {
    var value = json.get(FQM_INSTANCE_CHILD_INSTANCES_KEY);
    var childInstances =
        nonNull(value) ? objectMapper.readTree(value.toString()) : objectMapper.createArrayNode();
    jsonNode.putIfAbsent(CHILD_INSTANCES, childInstances);
    value = json.get(FQM_INSTANCE_PARENT_INSTANCES_KEY);
    var parentInstances =
        nonNull(value) ? objectMapper.readTree(value.toString()) : objectMapper.createArrayNode();
    jsonNode.putIfAbsent(PARENT_INSTANCES, parentInstances);
    value = json.get(FQM_INSTANCE_PRECEDING_TITLES_KEY);
    var precedingTitles =
        nonNull(value) ? objectMapper.readTree(value.toString()) : objectMapper.createArrayNode();
    jsonNode.putIfAbsent(PRECEDING_TITLES, precedingTitles);
    value = json.get(FQM_INSTANCE_SUCCEEDING_TITLES_KEY);
    var succeedingTitles =
        nonNull(value) ? objectMapper.readTree(value.toString()) : objectMapper.createArrayNode();
    jsonNode.putIfAbsent(SUCCEEDING_TITLES, succeedingTitles);
  }

  private boolean sharedMarcFromMemberHasNoHoldings(
    Map<String, Object> json, EntityType entityType, JsonNode jsonNode) {
    log.info("json: {}", json);
    log.info("entityType: {}", entityType);
    log.info("jsonNode: {}", jsonNode);
    boolean isMemberTenant = consortiaService.isTenantMember(folioExecutionContext.getTenantId());
    log.info("isMemberTenant: {}", isMemberTenant);
    log.info("shared: {}, {}", json.get(FQM_INSTANCE_SHARED_KEY), SHARED.equalsIgnoreCase(
      ofNullable(json.get(FQM_INSTANCE_SHARED_KEY)).map(Object::toString).orElse(EMPTY)));
    if (isMemberTenant && entityType == EntityType.INSTANCE_MARC
      && SHARED.equalsIgnoreCase(
      ofNullable(json.get(FQM_INSTANCE_SHARED_KEY)).map(Object::toString).orElse(EMPTY))) {
      var withHoldings =
        ofNullable(json.get(FQM_INSTANCES_WITH_HOLDINGS_KEY))
          .map(Object::toString).map(Boolean::parseBoolean)
          .orElse(false);
      log.info("withHoldings: {}", withHoldings);
      return !withHoldings;
    }
    return false;
  }
}
