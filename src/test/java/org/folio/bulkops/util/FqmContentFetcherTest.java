package org.folio.bulkops.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.bulkops.domain.bean.StateType.FAILED;
import static org.folio.bulkops.util.Constants.NO_MATCH_FOUND_MESSAGE;
import static org.folio.bulkops.util.FqmKeys.FQM_HOLDINGS_CALL_NUMBER_KEY;
import static org.folio.bulkops.util.FqmKeys.FQM_HOLDINGS_CALL_NUMBER_PREFIX_KEY;
import static org.folio.bulkops.util.FqmKeys.FQM_HOLDINGS_CALL_NUMBER_SUFFIX_KEY;
import static org.folio.bulkops.util.FqmKeys.FQM_INSTANCE_PUBLICATION_KEY;
import static org.folio.bulkops.util.FqmKeys.FQM_INSTANCES_TITLE_KEY;
import static org.folio.bulkops.util.FqmKeys.FQM_INSTANCE_TITLE_KEY;
import static org.folio.bulkops.util.FqmKeys.FQM_INSTANCES_PUBLICATION_KEY;
import static org.folio.bulkops.util.FqmKeys.FQM_HOLDING_PERMANENT_LOCATION_NAME_KEY;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.IntStream;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import feign.Request;
import feign.Response;
import org.folio.bulkops.BaseTest;
import org.folio.bulkops.client.QueryClient;
import org.folio.bulkops.domain.bean.StateType;
import org.folio.bulkops.domain.dto.EntityType;
import org.folio.bulkops.domain.entity.BulkOperationExecutionContent;
import org.folio.bulkops.exception.FqmFetcherException;
import org.folio.bulkops.service.ConsortiaService;
import org.folio.querytool.domain.dto.QueryDetails;
import org.folio.spring.FolioExecutionContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
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

  @MockitoBean
  private FolioExecutionContext folioExecutionContext;

  @MockitoBean
  private ConsortiaService consortiaService;

  @Autowired
  public ObjectMapper objectMapper;

  @Test
  void fetchShouldProcessMultipleChunksInParallel() throws IOException {

    var queryId = UUID.randomUUID();
    var data = getMockedData(0, Integer.MAX_VALUE);
    var total = data.getContent().size();
    var operationId = UUID.randomUUID();
    List<BulkOperationExecutionContent> contents = new ArrayList<>();

    var expected = data.getContent().stream().map(json -> {
        JsonNode instanceJsonb;
        try {
            instanceJsonb = objectMapper.readTree(json.get("instance.jsonb").toString());
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        var record1 = objectMapper.createObjectNode();
      record1.set("entity", instanceJsonb);
      record1.put("tenantId", "test_tenant");
      return record1.toString();
    }).toList();

    IntStream.range(0, (total + chunkSize - 1) / chunkSize).forEach(chunk -> {
      int offset = chunk * chunkSize;
      int limit = Math.min(chunkSize, total - offset);
      when(queryClient.getQuery(queryId, offset, limit)).thenReturn(getMockedData(offset, limit));
      when(folioExecutionContext.getTenantId()).thenReturn("test_tenant");
    });

    try (var is = fqmContentFetcher.fetch(queryId, EntityType.INSTANCE, total, contents, operationId)) {
      var actual = new String(is.readAllBytes(), StandardCharsets.UTF_8);
      assertThat(actual).contains(expected);
    }
  }

  @Test
  void fetchShouldFailMultipleChunksInParallel() {

    var queryId = UUID.randomUUID();
    var data = getMockedData(0, Integer.MAX_VALUE);
    var total = data.getContent().size();
    var operationId = UUID.randomUUID();
    List<BulkOperationExecutionContent> contents = new ArrayList<>();

    IntStream.range(0, ((total + chunkSize - 1) / chunkSize) - 1).forEach(chunk -> {
      int offset = chunk * chunkSize;
      int limit = Math.min(chunkSize, total - offset);
      when(queryClient.getQuery(queryId, offset, limit)).thenReturn(getMockedData(offset, limit));
      when(folioExecutionContext.getTenantId()).thenReturn("test_tenant");
    });

    int offset =  ((total + chunkSize - 1) / chunkSize - 1) * chunkSize;
    int limit = Math.min(chunkSize, total - offset);

    doThrow(FeignException.errorStatus("", Response.builder().status(500)
        .reason("GET-Error")
        .request(Request.create(Request.HttpMethod.GET, "", Map.of(), new byte[]{}, Charset.defaultCharset(), null))
        .build())).when(queryClient).getQuery(queryId, offset, limit);

    Exception exception = assertThrows(FqmFetcherException.class, () -> fqmContentFetcher.fetch(queryId, EntityType.INSTANCE, total, contents, operationId));

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

  @Test
  void fetchReturnsCorrectContentForUserEntityType() throws Exception {
    var queryId = UUID.randomUUID();
    int total = 2;
    var operationId = UUID.randomUUID();
    List<BulkOperationExecutionContent> contents = new ArrayList<>();
    when(folioExecutionContext.getTenantId()).thenReturn("tenant");
    when(queryClient.getQuery(queryId, 0, total)).thenReturn(getMockedDataForEntityType(EntityType.USER, total));

    try (var is = fqmContentFetcher.fetch(queryId, EntityType.USER, total, contents, operationId)) {
      var result = new String(is.readAllBytes(), StandardCharsets.UTF_8);
      assertThat(result).doesNotContain("\"entity\"");
      assertThat(result).doesNotContain("\"tenantId\":\"tenant\"");
      assertThat(result).contains("\"id\":\"user-id-0\"");
    }
  }

  @Test
  void fetchReturnsCorrectContentForItemEntityType() throws Exception {
    var queryId = UUID.randomUUID();
    int total = 2;
    var operationId = UUID.randomUUID();
    List<BulkOperationExecutionContent> contents = new ArrayList<>();
    when(folioExecutionContext.getTenantId()).thenReturn("tenant");
    when(queryClient.getQuery(queryId, 0, total)).thenReturn(getMockedDataForEntityType(EntityType.ITEM, total));

    try (var is = fqmContentFetcher.fetch(queryId, EntityType.ITEM, total, contents, operationId)) {
      var result = new String(is.readAllBytes(), StandardCharsets.UTF_8);
      assertThat(result).contains("\"entity\"");
      assertThat(result).contains("\"tenantId\":\"item-tenant\"");
      assertThat(result).contains("\"title\":\"Instance Title 1. Olaf Ladousse, 1992-\"");
    }
  }

  @Test
  void fetchReturnsCorrectContentForHoldingsRecordEntityType() throws Exception {
    var queryId = UUID.randomUUID();
    int total = 2;
    var operationId = UUID.randomUUID();
    List<BulkOperationExecutionContent> contents = new ArrayList<>();
    when(folioExecutionContext.getTenantId()).thenReturn("tenant");
    when(queryClient.getQuery(queryId, 0, total)).thenReturn(getMockedDataForEntityType(EntityType.HOLDINGS_RECORD, total));

    try (var is = fqmContentFetcher.fetch(queryId, EntityType.HOLDINGS_RECORD, total, contents, operationId)) {
      var result = new String(is.readAllBytes(), StandardCharsets.UTF_8);
      assertThat(result).contains("\"entity\"");
      assertThat(result).contains("\"tenantId\":\"holdings-tenant\"");
      assertThat(result).contains("\"instanceTitle\":\"Instance Title 0. Olaf Ladousse, 1992-\"");
    }
  }

  @Test
  void fetchReturnsCorrectContentForInstanceEntityType() throws Exception {
    var queryId = UUID.randomUUID();
    int total = 2;
    var operationId = UUID.randomUUID();
    List<BulkOperationExecutionContent> contents = new ArrayList<>();
    when(folioExecutionContext.getTenantId()).thenReturn("tenant");
    when(queryClient.getQuery(queryId, 0, total)).thenReturn(getMockedDataForEntityType(EntityType.INSTANCE, total));

    try (var is = fqmContentFetcher.fetch(queryId, EntityType.INSTANCE, total, contents, operationId)) {
      var result = new String(is.readAllBytes(), StandardCharsets.UTF_8);
      assertThat(result).contains("\"entity\"");
      assertThat(result).contains("\"tenantId\":\"instance-tenant\"");
    }
  }

  @ParameterizedTest
  @EnumSource(value = EntityType.class, names = {"INSTANCE", "INSTANCE_MARC"}, mode = EnumSource.Mode.INCLUDE)
  void fetchShouldReturnAnErrorForNonSharedInstancesInEcs(EntityType entityType) throws Exception {
    var queryId = UUID.randomUUID();
    int total = 2;
    var operationId = UUID.randomUUID();
    List<BulkOperationExecutionContent> contents = new ArrayList<>();

    when(folioExecutionContext.getTenantId()).thenReturn("tenant");
    when(consortiaService.isTenantCentral(anyString())).thenReturn(true);

    var details = new QueryDetails();
    Map<String, Object> map = new HashMap<>();
    map.put("instance.shared", "Local");
    details.setContent(Collections.singletonList(map));
    when(queryClient.getQuery(queryId, 0, total)).thenReturn(details);

    try (var is = fqmContentFetcher.fetch(queryId, entityType, total, contents, operationId)) {
      var result = new String(is.readAllBytes(), StandardCharsets.UTF_8);
      assertThat(contents).hasSize(1);
      assertThat(contents.getFirst().getState()).isEqualTo(FAILED);
      assertThat(contents.getFirst().getErrorMessage()).isEqualTo(NO_MATCH_FOUND_MESSAGE);
      assertThat(result).isEmpty();
    }
  }

  @Test
  void fetchReturnsCorrectContentForInstanceMarcEntityType() throws Exception {
    var queryId = UUID.randomUUID();
    int total = 2;
    var operationId = UUID.randomUUID();
    List<BulkOperationExecutionContent> contents = new ArrayList<>();
    when(folioExecutionContext.getTenantId()).thenReturn("tenant");
    when(queryClient.getQuery(queryId, 0, total)).thenReturn(getMockedDataForEntityType(EntityType.INSTANCE_MARC, total));

    try (var is = fqmContentFetcher.fetch(queryId, EntityType.INSTANCE_MARC, total, contents, operationId)) {
      var result = new String(is.readAllBytes(), StandardCharsets.UTF_8);
      assertThat(result).contains("\"entity\"");
      assertThat(result).contains("\"tenantId\":\"instance-tenant\"");
    }
  }

    @Test
    void fetchAddsWarningForCentralTenantAndNonUserEntity() throws Exception {
      var queryId = UUID.randomUUID();
      int total = 1;
      var operationId = UUID.randomUUID();
      List<BulkOperationExecutionContent> contents = new ArrayList<>();
      when(folioExecutionContext.getTenantId()).thenReturn("central-tenant");
      when(consortiaService.isTenantCentral("central-tenant")).thenReturn(true);
      var mockForInstanceWithoutTenant = getMockedDataForEntityType(EntityType.INSTANCE, total);
      mockForInstanceWithoutTenant.getContent().forEach(map -> map.remove("instance.tenant_id"));
      when(queryClient.getQuery(queryId, 0, total)).thenReturn(mockForInstanceWithoutTenant);

      try (var is = fqmContentFetcher.fetch(queryId, EntityType.INSTANCE, total, contents, operationId)) {
        is.readAllBytes();
      }

      assertThat(contents).hasSize(1);
      BulkOperationExecutionContent content = contents.getFirst();
      assertThat(content.getIdentifier()).isEqualTo("instance-id-0");
      assertThat(content.getBulkOperationId()).isEqualTo(operationId);
      assertThat(content.getState()).isEqualTo(StateType.PROCESSED);
      assertThat(content.getErrorType()).isEqualTo(org.folio.bulkops.domain.dto.ErrorType.WARNING);
      assertThat(content.getErrorMessage()).contains("tenant field is missing");
    }

    @Test
    void fetchDoesNotAddWarningForNonCentralTenant() throws Exception {
      var queryId = UUID.randomUUID();
      int total = 1;
      var operationId = UUID.randomUUID();
      List<BulkOperationExecutionContent> contents = new ArrayList<>();
      when(folioExecutionContext.getTenantId()).thenReturn("member-tenant");
      when(consortiaService.isTenantCentral("member-tenant")).thenReturn(false);
      when(queryClient.getQuery(queryId, 0, total)).thenReturn(getMockedDataForEntityType(EntityType.INSTANCE, total));

      try (var is = fqmContentFetcher.fetch(queryId, EntityType.INSTANCE, total, contents, operationId)) {
        is.readAllBytes();
      }

      assertThat(contents).isEmpty();
    }

    @Test
    void fetchDoesNotAddWarningForUserEntityTypeIfNotCentralTenant() throws Exception {
      var queryId = UUID.randomUUID();
      int total = 1;
      var operationId = UUID.randomUUID();
      List<BulkOperationExecutionContent> contents = new ArrayList<>();
      when(folioExecutionContext.getTenantId()).thenReturn("member-tenant");
      when(consortiaService.isTenantCentral("member-tenant")).thenReturn(false);
      when(queryClient.getQuery(queryId, 0, total)).thenReturn(getMockedDataForEntityType(EntityType.USER, total));

      try (var is = fqmContentFetcher.fetch(queryId, EntityType.USER, total, contents, operationId)) {
        is.readAllBytes();
      }

      assertThat(contents).isEmpty();
    }

  // Helper for mocking QueryDetails for each EntityType
  private QueryDetails getMockedDataForEntityType(EntityType entityType, int total) {
    var details = new QueryDetails();
    var contentList = new ArrayList<Map<String, Object>>();
    for (int i = 0; i < total; i++) {
      var map = new HashMap<String, Object>();
      switch (entityType) {
        case USER -> map.put("users.jsonb", "{\"id\":\"user-id-" + i + "\"}");
        case ITEM -> {
          map.put("items.jsonb", "{\"id\":\"item-id-" + i + "\"}");
          map.put("items.tenant_id", "item-tenant");
          map.put(FQM_INSTANCES_TITLE_KEY, "Instance Title " + i);
          map.put(FQM_HOLDINGS_CALL_NUMBER_PREFIX_KEY, "Call/Number:Prefix " + i);
          map.put(FQM_HOLDINGS_CALL_NUMBER_SUFFIX_KEY, "Call Number-Suffix " + i);
          map.put(FQM_HOLDINGS_CALL_NUMBER_KEY, "CN_" + i);
          map.put(FQM_HOLDING_PERMANENT_LOCATION_NAME_KEY, "Main");
          map.put(FQM_INSTANCES_PUBLICATION_KEY, "[{\"place\": \"Madrid\", \"publisher\": \"Olaf Ladousse\", \"dateOfPublication\": \"1992-\"}]");
        }
        case HOLDINGS_RECORD -> {
          map.put("holdings.jsonb", "{\"id\":\"holdings-id-" + i + "\"}");
          map.put("holdings.tenant_id", "holdings-tenant");
          map.put(FQM_INSTANCE_TITLE_KEY, "Instance Title " + i);
          map.put(FQM_INSTANCE_PUBLICATION_KEY, "[{\"place\": \"Madrid\", \"publisher\": \"Olaf Ladousse\", \"dateOfPublication\": \"1992-\"}]");
        }
        case INSTANCE, INSTANCE_MARC -> {
          map.put("instance.jsonb", "{\"id\":\"instance-id-" + i + "\"}");
          map.put("instance.tenant_id", "instance-tenant");
          map.put("instance.shared", "Shared");
        }
      }
      contentList.add(map);
    }
    details.setContent(contentList);
    return details;
  }
}
