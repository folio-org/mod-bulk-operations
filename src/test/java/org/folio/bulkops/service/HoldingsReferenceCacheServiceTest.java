package org.folio.bulkops.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.spring.integration.XOkapiHeaders.TENANT;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.folio.bulkops.client.HoldingsStorageClient;
import org.folio.bulkops.client.LocationClient;
import org.folio.spring.FolioExecutionContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class HoldingsReferenceCacheServiceTest {

  @Mock
  private HoldingsStorageClient holdingsStorageClient;
  @Mock
  private LocationClient locationClient;
  @Mock
  private FolioExecutionContext folioExecutionContext;
  @InjectMocks
  private HoldingsReferenceCacheService holdingsReferenceCacheService;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    when(folioExecutionContext.getOkapiHeaders()).thenReturn(Map.of(TENANT, List.of("diku")));
  }

  @Test
  void getHoldingsJsonById_returnsJson() {
    ObjectMapper mapper = new ObjectMapper();
    JsonNode holdingsJson = mapper.createObjectNode().put("id", "h1");
    when(holdingsStorageClient.getHoldingsJsonById("h1")).thenReturn(holdingsJson);

    JsonNode result = holdingsReferenceCacheService.getHoldingsJsonById("h1", "tenant");
    assertThat(result.get("id").asText()).isEqualTo("h1");
  }

  @Test
  void getHoldingsLocationById_returnsEmptyObjectForEmptyId() {
    JsonNode result = holdingsReferenceCacheService.getHoldingsLocationById("", "tenant");
    assertThat(result.isObject()).isTrue();
    assertThat(result.size()).isZero();
  }

  @Test
  void getHoldingsLocationById_returnsJson() {
    ObjectMapper mapper = new ObjectMapper();
    JsonNode locationJson = mapper.createObjectNode().put("id", "loc1");
    when(locationClient.getLocationJsonById("loc1")).thenReturn(locationJson);

    JsonNode result = holdingsReferenceCacheService.getHoldingsLocationById("loc1", "tenant");
    assertThat(result.get("id").asText()).isEqualTo("loc1");
  }
}
