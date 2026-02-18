package org.folio.bulkops.util;

import static java.lang.String.join;
import static java.util.Collections.emptyList;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.SPACE;
import static org.folio.bulkops.domain.dto.EntityType.HOLDINGS_RECORD;
import static org.folio.bulkops.domain.dto.EntityType.ITEM;
import static org.folio.bulkops.domain.dto.EntityType.USER;
import static org.folio.bulkops.util.Constants.CHILD_INSTANCES;
import static org.folio.bulkops.util.Constants.EFFECTIVE_LOCATION;
import static org.folio.bulkops.util.Constants.ENTITY;
import static org.folio.bulkops.util.Constants.HOLDINGS_DATA;
import static org.folio.bulkops.util.Constants.HOLDINGS_LOCATION_CALL_NUMBER_DELIMITER;
import static org.folio.bulkops.util.Constants.INSTANCE_TITLE;
import static org.folio.bulkops.util.Constants.LINE_BREAK;
import static org.folio.bulkops.util.Constants.LINKED_DATA_SOURCE;
import static org.folio.bulkops.util.Constants.LINKED_DATA_SOURCE_IS_NOT_SUPPORTED;
import static org.folio.bulkops.util.Constants.MSG_SHADOW_RECORDS_CANNOT_BE_EDITED;
import static org.folio.bulkops.util.Constants.NO_MATCH_FOUND_MESSAGE;
import static org.folio.bulkops.util.Constants.PARENT_INSTANCES;
import static org.folio.bulkops.util.Constants.PERMANENT_LOAN_TYPE;
import static org.folio.bulkops.util.Constants.PERMANENT_LOCATION;
import static org.folio.bulkops.util.Constants.PRECEDING_TITLES;
import static org.folio.bulkops.util.Constants.SUCCEEDING_TITLES;
import static org.folio.bulkops.util.Constants.TEMPORARY_LOAN_TYPE;
import static org.folio.bulkops.util.Constants.TEMPORARY_LOCATION;
import static org.folio.bulkops.util.Constants.TENANT_ID;
import static org.folio.bulkops.util.Constants.TITLE;
import static org.folio.bulkops.util.FqmKeys.FQM_DATE_OF_PUBLICATION_KEY;
import static org.folio.bulkops.util.FqmKeys.FQM_HOLDINGS_CALL_NUMBER_KEY;
import static org.folio.bulkops.util.FqmKeys.FQM_HOLDINGS_CALL_NUMBER_PREFIX_KEY;
import static org.folio.bulkops.util.FqmKeys.FQM_HOLDINGS_CALL_NUMBER_SUFFIX_KEY;
import static org.folio.bulkops.util.FqmKeys.FQM_HOLDINGS_JSONB_KEY;
import static org.folio.bulkops.util.FqmKeys.FQM_HOLDINGS_TENANT_ID_KEY;
import static org.folio.bulkops.util.FqmKeys.FQM_HOLDING_PERMANENT_LOCATION_NAME_KEY;
import static org.folio.bulkops.util.FqmKeys.FQM_INSTANCES_PUBLICATION_KEY;
import static org.folio.bulkops.util.FqmKeys.FQM_INSTANCES_TITLE_KEY;
import static org.folio.bulkops.util.FqmKeys.FQM_INSTANCE_CHILD_INSTANCES_KEY;
import static org.folio.bulkops.util.FqmKeys.FQM_INSTANCE_ID_KEY;
import static org.folio.bulkops.util.FqmKeys.FQM_INSTANCE_JSONB_KEY;
import static org.folio.bulkops.util.FqmKeys.FQM_INSTANCE_PARENT_INSTANCES_KEY;
import static org.folio.bulkops.util.FqmKeys.FQM_INSTANCE_PRECEDING_TITLES_KEY;
import static org.folio.bulkops.util.FqmKeys.FQM_INSTANCE_PUBLICATION_KEY;
import static org.folio.bulkops.util.FqmKeys.FQM_INSTANCE_SHARED_KEY;
import static org.folio.bulkops.util.FqmKeys.FQM_INSTANCE_SOURCE_KEY;
import static org.folio.bulkops.util.FqmKeys.FQM_INSTANCE_SUCCEEDING_TITLES_KEY;
import static org.folio.bulkops.util.FqmKeys.FQM_INSTANCE_TENANT_ID_KEY;
import static org.folio.bulkops.util.FqmKeys.FQM_INSTANCE_TITLE_KEY;
import static org.folio.bulkops.util.FqmKeys.FQM_ITEMS_JSONB_KEY;
import static org.folio.bulkops.util.FqmKeys.FQM_ITEMS_TENANT_ID_KEY;
import static org.folio.bulkops.util.FqmKeys.FQM_ITEM_EFFECTIVE_LOCATION_ID_KEY;
import static org.folio.bulkops.util.FqmKeys.FQM_ITEM_EFFECTIVE_LOCATION_NAME_KEY;
import static org.folio.bulkops.util.FqmKeys.FQM_ITEM_PERMANENT_LOAN_TYPE_ID_KEY;
import static org.folio.bulkops.util.FqmKeys.FQM_ITEM_PERMANENT_LOAN_TYPE_NAME_KEY;
import static org.folio.bulkops.util.FqmKeys.FQM_ITEM_PERMANENT_LOCATION_ID_KEY;
import static org.folio.bulkops.util.FqmKeys.FQM_ITEM_PERMANENT_LOCATION_NAME_KEY;
import static org.folio.bulkops.util.FqmKeys.FQM_ITEM_TEMPORARY_LOAN_TYPE_ID_KEY;
import static org.folio.bulkops.util.FqmKeys.FQM_ITEM_TEMPORARY_LOAN_TYPE_NAME_KEY;
import static org.folio.bulkops.util.FqmKeys.FQM_ITEM_TEMPORARY_LOCATION_ID_KEY;
import static org.folio.bulkops.util.FqmKeys.FQM_ITEM_TEMPORARY_LOCATION_NAME_KEY;
import static org.folio.bulkops.util.FqmKeys.FQM_PUBLISHER_KEY;
import static org.folio.bulkops.util.FqmKeys.FQM_USERS_ID_KEY;
import static org.folio.bulkops.util.FqmKeys.FQM_USERS_JSONB_KEY;
import static org.folio.bulkops.util.FqmKeys.FQM_USERS_TYPE_KEY;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.annotation.PostConstruct;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.SequenceInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.folio.bulkops.client.QueryClient;
import org.folio.bulkops.client.SearchClient;
import org.folio.bulkops.domain.bean.StateType;
import org.folio.bulkops.domain.dto.BatchIdsDto;
import org.folio.bulkops.domain.dto.BatchIdsDto.IdentifierTypeEnum;
import org.folio.bulkops.domain.dto.ConsortiumHolding;
import org.folio.bulkops.domain.dto.ConsortiumItem;
import org.folio.bulkops.domain.dto.EntityType;
import org.folio.bulkops.domain.dto.ErrorType;
import org.folio.bulkops.domain.entity.BulkOperationExecutionContent;
import org.folio.bulkops.exception.FqmFetcherException;
import org.folio.bulkops.service.ConsortiaService;
import org.folio.bulkops.service.EntityTypeService;
import org.folio.querytool.domain.dto.ContentsRequest;
import org.folio.spring.FolioExecutionContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Log4j2
@RequiredArgsConstructor
public class FqmContentFetcher {

  public static final String SHARED = "Shared";
  public static final String SHADOW = "shadow";
  public static final String MARC = "MARC";

  private static final int BUFFER_SIZE = 64 * 1024;
  public static final String ID = "id";
  public static final String NAME = "name";

  @Value("${application.fqm-fetcher.max_parallel_chunks}")
  private int maxParallelChunks;

  @Value("${application.fqm-fetcher.max_chunk_size}")
  private int chunkSize;

  private final QueryClient queryClient;
  private final ObjectMapper objectMapper;
  private final FolioExecutionContext folioExecutionContext;
  private final ConsortiaService consortiaService;
  private final EntityTypeService entityTypeService;
  private final SearchClient searchClient;

  @PostConstruct
  private void logStartup() {
    log.info(
        "FqmContentFetcher initialized with parameters - max_parallel_chunks: {}, "
            + "max_chunk_size: {}",
        maxParallelChunks,
        chunkSize);
  }

  /**
   * Fetches content of FQM entities for the given UUIDs.
   *
   * @param uuids the list of UUIDs to fetch
   * @param entityType the type of entity to fetch
   * @param bulkOperationExecutionContents the list to record execution content for errors or
   *     warnings (technical errors holder)
   * @param operationId the bulk operation ID for which the content is fetched
   * @return an InputStream containing the fetched content
   */
  public InputStream contents(
      List<UUID> uuids,
      EntityType entityType,
      List<BulkOperationExecutionContent> bulkOperationExecutionContents,
      UUID operationId) {

    UUID entityTypeId = entityTypeService.getEntityTypeIdByBulkOpsEntityType(entityType);

    List<String> entityJsonKeys = getEntityJsonKeys(entityType);

    var tenantId = folioExecutionContext.getTenantId();

    List<List<UUID>> chunks =
        IntStream.range(0, uuids.size())
            .filter(i -> i % chunkSize == 0)
            .mapToObj(i -> uuids.subList(i, Math.min(i + chunkSize, uuids.size())))
            .toList();

    ExecutorService pool = Executors.newFixedThreadPool(maxParallelChunks);
    CompletionService<List<Map<String, Object>>> completion = new ExecutorCompletionService<>(pool);

    String centralTenantId = consortiaService.getCentralTenantId(tenantId);
    boolean isTenantInConsortia = StringUtils.isNotEmpty(centralTenantId);
    boolean isCentralTenant = tenantId.equals(centralTenantId);

    for (List<UUID> chunk : chunks) {

      Function<String, List<String>> idMapper;

      if (isTenantInConsortia && entityType != USER) {
        if (isCentralTenant && (ITEM == entityType || HOLDINGS_RECORD == entityType)) {
          var batchIdsDto =
              new BatchIdsDto()
                  .identifierType(IdentifierTypeEnum.ID)
                  .identifierValues(chunk.stream().map(UUID::toString).toList());

          Map<String, String> idTenantMap =
              switch (entityType) {
                case ITEM ->
                    searchClient.getConsortiumItemCollection(batchIdsDto).getItems().stream()
                        .collect(
                            Collectors.toMap(
                                ConsortiumItem::getId,
                                v -> Optional.ofNullable(v.getTenantId()).orElse(EMPTY)));
                case HOLDINGS_RECORD ->
                    searchClient.getConsortiumHoldingCollection(batchIdsDto).getHoldings().stream()
                        .collect(
                            Collectors.toMap(
                                ConsortiumHolding::getId,
                                v -> Optional.ofNullable(v.getTenantId()).orElse(EMPTY)));
                default -> Map.of();
              };
          idMapper = id -> List.of(id, idTenantMap.getOrDefault(id, EMPTY));
        } else {
          idMapper = id -> List.of(id, tenantId);
        }
      } else {
        idMapper = List::of;
      }

      completion.submit(
          () -> {
            ContentsRequest req =
                new ContentsRequest()
                    .entityTypeId(entityTypeId)
                    .fields(entityJsonKeys)
                    .ids(chunk.stream().map(UUID::toString).map(idMapper).toList());
            return queryClient.getContents(req);
          });
    }

    return pipeStreamingResponse(
        entityType,
        bulkOperationExecutionContents,
        operationId,
        isCentralTenant,
        chunks.size(),
        completion,
        pool);
  }

  private InputStream pipeStreamingResponse(
      EntityType entityType,
      List<BulkOperationExecutionContent> bulkOperationExecutionContents,
      UUID operationId,
      boolean isCentralTenant,
      int submittedTasks,
      CompletionService<List<Map<String, Object>>> completion,
      ExecutorService executor) {

    final PipedOutputStream pos = new PipedOutputStream();
    final PipedInputStream pis;
    try {
      pis = new PipedInputStream(pos, BUFFER_SIZE);
    } catch (IOException e) {
      executor.shutdownNow();
      throw new FqmFetcherException("Error creating piped streams for FQM contents", e);
    }

    final AtomicReference<Throwable> error = new AtomicReference<>(null);

    var writer =
        new Thread(
            () -> {
              try (BufferedWriter bw =
                  new BufferedWriter(
                      new OutputStreamWriter(pos, StandardCharsets.UTF_8), BUFFER_SIZE)) {

                for (int i = 0; i < submittedTasks; i++) {
                  List<Map<String, Object>> part = next(completion, error, executor);

                  if (part == null || part.isEmpty()) {
                    continue;
                  }

                  part(
                      bw,
                      part,
                      entityType,
                      bulkOperationExecutionContents,
                      operationId,
                      isCentralTenant);
                }

              } catch (InterruptedException ie) {
                log.error("Error streaming FQM response", ie);
                fail(error, executor, ie, true);
              } catch (Exception ex) {
                log.error("Error streaming FQM response", ex);
                fail(error, executor, ex, false);
              } finally {
                closeQuietly(pos);
                executor.shutdownNow();
              }
            });

    writer.setDaemon(true);
    writer.start();

    return new FilterInputStream(pis) {

      @Override
      public int read() throws IOException {
        checkError();
        int result = super.read();
        if (result == -1) {
          checkError();
        }
        return result;
      }

      @Override
      public int read(byte[] b, int off, int len) throws IOException {
        checkError();
        int result = super.read(b, off, len);
        if (result == -1) {
          checkError();
        }
        return result;
      }

      @Override
      public void close() throws IOException {
        try {
          super.close();
        } finally {
          executor.shutdownNow();
          checkError();
        }
      }

      private void checkError() throws IOException {
        Throwable t = unwrap(error.get());
        if (t != null) {
          throw new IOException(new FqmFetcherException("Error retrieving contents from FQM", t));
        }
      }
    };
  }

  private List<Map<String, Object>> next(
      CompletionService<List<Map<String, Object>>> completion,
      AtomicReference<Throwable> error,
      ExecutorService executor)
      throws InterruptedException {

    try {
      return completion.take().get();
    } catch (InterruptedException ie) {
      fail(error, executor, ie, true);
      throw ie;
    } catch (Exception ex) {
      fail(error, executor, ex, false);
      return emptyList();
    }
  }

  private void part(
      BufferedWriter bw,
      List<Map<String, Object>> part,
      EntityType entityType,
      List<BulkOperationExecutionContent> bulkOperationExecutionContents,
      UUID operationId,
      boolean isCentralTenant)
      throws IOException {

    for (Map<String, Object> json : part) {
      String line =
          processFqmJson(
              entityType, bulkOperationExecutionContents, operationId, isCentralTenant, json);

      if (StringUtils.isNotEmpty(line)) {
        bw.write(line);
        bw.write(LINE_BREAK);
      }
    }
    bw.flush();
  }

  private void fail(
      AtomicReference<Throwable> error, ExecutorService executor, Throwable t, boolean interrupt) {

    error.compareAndSet(null, t);

    if (interrupt) {
      Thread.currentThread().interrupt();
    }

    executor.shutdownNow();
  }

  private Throwable unwrap(Throwable throwable) {
    if (throwable instanceof ExecutionException ee && ee.getCause() != null) {
      return ee.getCause();
    }
    if (throwable instanceof CompletionException ce && ce.getCause() != null) {
      return ce.getCause();
    }
    return throwable;
  }

  private void closeQuietly(Closeable closeable) {
    try {
      closeable.close();
    } catch (IOException ignore) {
      // ignore
    }
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
      log.error("Error fetching data from FQM", e);
      throw new FqmFetcherException("Error fetching data from FQM", e);
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
    log.info("tenant current: {}", folioExecutionContext.getTenantId());
    var response = queryClient.getQuery(queryId, offset, limit).getContent();
    return getFqmResponseAsInputStream(
        entityType, bulkOperationExecutionContents, operationId, isCentralTenant, response);
  }

  protected InputStream getFqmResponseAsInputStream(
      EntityType entityType,
      List<BulkOperationExecutionContent> bulkOperationExecutionContents,
      UUID operationId,
      boolean isCentralTenant,
      List<Map<String, Object>> response) {
    return new ByteArrayInputStream(
        response.stream()
            .map(
                json ->
                    processFqmJson(
                        entityType,
                        bulkOperationExecutionContents,
                        operationId,
                        isCentralTenant,
                        json))
            .filter(StringUtils::isNotEmpty)
            .collect(Collectors.joining(LINE_BREAK))
            .getBytes(StandardCharsets.UTF_8));
  }

  private String processFqmJson(
      EntityType entityType,
      List<BulkOperationExecutionContent> bulkOperationExecutionContents,
      UUID operationId,
      boolean isCentralTenant,
      Map<String, Object> json) {
    try {
      if (isInstance(entityType)
          && LINKED_DATA_SOURCE.equalsIgnoreCase(
              ofNullable(json.get(FQM_INSTANCE_SOURCE_KEY)).map(Object::toString).orElse(EMPTY))) {
        addInstanceLinkedDataNotSupported(json, bulkOperationExecutionContents, operationId);
        return EMPTY;
      }
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
                ofNullable(json.get(FQM_USERS_TYPE_KEY)).map(Object::toString).orElse(EMPTY))) {
          addShadowUserErrorContent(json, bulkOperationExecutionContents, operationId);
          return EMPTY;
        }
      }

      if (isSharedInstanceAndCurrentTenantIsMember(json, entityType)) {
        addInstanceNoMatchFound(json, bulkOperationExecutionContents, operationId);
        return EMPTY;
      }

      var jsonb = json.get(getEntityJsonKey(entityType));

      if (Objects.nonNull(jsonb)) {
        if (entityType == EntityType.USER) {
          return jsonb.toString();
        }
        var jsonNode = (ObjectNode) objectMapper.readTree(jsonb.toString());
        var tenant = json.get(getContentTenantKey(entityType));
        if (tenant == null) {
          checkForTenantFieldExistenceInEcs(
              jsonNode.get(Constants.ID).asText(), operationId, bulkOperationExecutionContents);
          tenant = folioExecutionContext.getTenantId();
        }
        jsonNode.put(TENANT_ID, tenant.toString());

        if (entityType == ITEM) {
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
      } else {
        return EMPTY;
      }
    } catch (Exception e) {
      log.error("Error processing JSON content: {}", json, e);
      return EMPTY;
    }
  }

  private String getEntityJsonKey(EntityType entityType) {
    return switch (entityType) {
      case USER -> FQM_USERS_JSONB_KEY;
      case ITEM -> FQM_ITEMS_JSONB_KEY;
      case HOLDINGS_RECORD -> FQM_HOLDINGS_JSONB_KEY;
      case INSTANCE, INSTANCE_MARC -> FQM_INSTANCE_JSONB_KEY;
    };
  }

  private List<String> getEntityJsonKeys(EntityType entityType) {
    return switch (entityType) {
      case USER -> List.of(FQM_USERS_JSONB_KEY, FQM_USERS_TYPE_KEY);
      case ITEM ->
          List.of(
              FQM_ITEMS_JSONB_KEY,
              FQM_INSTANCES_TITLE_KEY,
              FQM_HOLDINGS_CALL_NUMBER_PREFIX_KEY,
              FQM_HOLDINGS_CALL_NUMBER_SUFFIX_KEY,
              FQM_HOLDINGS_CALL_NUMBER_KEY,
              FQM_HOLDING_PERMANENT_LOCATION_NAME_KEY,
              FQM_INSTANCES_PUBLICATION_KEY,
              FQM_ITEM_PERMANENT_LOAN_TYPE_ID_KEY,
              FQM_ITEM_PERMANENT_LOAN_TYPE_NAME_KEY,
              FQM_ITEM_TEMPORARY_LOAN_TYPE_ID_KEY,
              FQM_ITEM_TEMPORARY_LOAN_TYPE_NAME_KEY,
              FQM_ITEM_TEMPORARY_LOAN_TYPE_NAME_KEY,
              FQM_ITEM_EFFECTIVE_LOCATION_ID_KEY,
              FQM_ITEM_EFFECTIVE_LOCATION_NAME_KEY,
              FQM_ITEM_PERMANENT_LOCATION_ID_KEY,
              FQM_ITEM_PERMANENT_LOCATION_NAME_KEY,
              FQM_ITEM_TEMPORARY_LOCATION_ID_KEY,
              FQM_ITEM_TEMPORARY_LOCATION_NAME_KEY,
              FQM_ITEMS_TENANT_ID_KEY);
      case HOLDINGS_RECORD ->
          List.of(
              FQM_HOLDINGS_JSONB_KEY,
              FQM_INSTANCE_TITLE_KEY,
              FQM_INSTANCE_PUBLICATION_KEY,
              FQM_HOLDINGS_TENANT_ID_KEY);
      case INSTANCE, INSTANCE_MARC ->
          List.of(
              FQM_INSTANCE_JSONB_KEY,
              FQM_INSTANCE_SHARED_KEY,
              FQM_INSTANCE_SOURCE_KEY,
              FQM_INSTANCE_TENANT_ID_KEY);
    };
  }

  private String getContentTenantKey(EntityType entityType) {
    return switch (entityType) {
      case USER -> StringUtils.EMPTY;
      case ITEM -> FQM_ITEMS_TENANT_ID_KEY;
      case HOLDINGS_RECORD -> FQM_HOLDINGS_TENANT_ID_KEY;
      case INSTANCE, INSTANCE_MARC -> FQM_INSTANCE_TENANT_ID_KEY;
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

  private void addInstanceLinkedDataNotSupported(
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
            .errorMessage(LINKED_DATA_SOURCE_IS_NOT_SUPPORTED)
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
                json.get(FQM_HOLDINGS_CALL_NUMBER_KEY),
                json.get(FQM_HOLDINGS_CALL_NUMBER_SUFFIX_KEY))
            .filter(Objects::nonNull)
            .map(Object::toString)
            .collect(Collectors.joining(SPACE));

    var permanentLocationName =
        ofNullable(json.get(FQM_HOLDING_PERMANENT_LOCATION_NAME_KEY)).orElse(EMPTY).toString();

    jsonNode.put(
        HOLDINGS_DATA,
        join(HOLDINGS_LOCATION_CALL_NUMBER_DELIMITER, permanentLocationName, callNumber));

    if (nonNull(json.get(FQM_ITEM_PERMANENT_LOAN_TYPE_ID_KEY))) {
      jsonNode.putIfAbsent(
          PERMANENT_LOAN_TYPE,
          objectMapper
              .createObjectNode()
              .put(ID, json.get(FQM_ITEM_PERMANENT_LOAN_TYPE_ID_KEY).toString())
              .put(
                  NAME,
                  ofNullable(json.get(FQM_ITEM_PERMANENT_LOAN_TYPE_NAME_KEY))
                      .orElse(EMPTY)
                      .toString()));
    }

    if (nonNull(json.get(FQM_ITEM_TEMPORARY_LOAN_TYPE_ID_KEY))) {
      jsonNode.putIfAbsent(
          TEMPORARY_LOAN_TYPE,
          objectMapper
              .createObjectNode()
              .put(ID, json.get(FQM_ITEM_TEMPORARY_LOAN_TYPE_ID_KEY).toString())
              .put(
                  NAME,
                  ofNullable(json.get(FQM_ITEM_TEMPORARY_LOAN_TYPE_NAME_KEY))
                      .orElse(EMPTY)
                      .toString()));
    }

    if (nonNull(json.get(FQM_ITEM_EFFECTIVE_LOCATION_ID_KEY))) {
      jsonNode.putIfAbsent(
          EFFECTIVE_LOCATION,
          objectMapper
              .createObjectNode()
              .put(ID, json.get(FQM_ITEM_EFFECTIVE_LOCATION_ID_KEY).toString())
              .put(
                  NAME,
                  ofNullable(json.get(FQM_ITEM_EFFECTIVE_LOCATION_NAME_KEY))
                      .orElse(EMPTY)
                      .toString()));
    }

    if (nonNull(json.get(FQM_ITEM_PERMANENT_LOCATION_ID_KEY))) {
      jsonNode.putIfAbsent(
          PERMANENT_LOCATION,
          objectMapper
              .createObjectNode()
              .put(ID, json.get(FQM_ITEM_PERMANENT_LOCATION_ID_KEY).toString())
              .put(
                  NAME,
                  ofNullable(json.get(FQM_ITEM_PERMANENT_LOCATION_NAME_KEY))
                      .orElse(EMPTY)
                      .toString()));
    }

    if (nonNull(json.get(FQM_ITEM_TEMPORARY_LOCATION_ID_KEY))) {
      jsonNode.putIfAbsent(
          TEMPORARY_LOCATION,
          objectMapper
              .createObjectNode()
              .put(ID, json.get(FQM_ITEM_TEMPORARY_LOCATION_ID_KEY).toString())
              .put(
                  NAME,
                  ofNullable(json.get(FQM_ITEM_TEMPORARY_LOCATION_NAME_KEY))
                      .orElse(EMPTY)
                      .toString()));
    }
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

  private boolean isSharedInstanceAndCurrentTenantIsMember(
      Map<String, Object> json, EntityType entityType) {
    boolean isMemberTenant = consortiaService.isTenantMember(folioExecutionContext.getTenantId());
    return isMemberTenant
        && isInstance(entityType)
        && SHARED.equalsIgnoreCase(
            ofNullable(json.get(FQM_INSTANCE_SHARED_KEY)).map(Object::toString).orElse(EMPTY));
  }
}
