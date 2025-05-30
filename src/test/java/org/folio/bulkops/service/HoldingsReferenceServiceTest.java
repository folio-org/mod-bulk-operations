package org.folio.bulkops.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.folio.spring.integration.XOkapiHeaders.TENANT;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.folio.bulkops.client.HoldingsClient;
import org.folio.bulkops.client.InstanceClient;
import org.folio.bulkops.client.LocationClient;
import org.folio.bulkops.domain.dto.ErrorType;
import org.folio.bulkops.exception.BulkEditException;
import org.folio.bulkops.exception.NotFoundException;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.FolioModuleMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Map;

class HoldingsReferenceServiceTest {

  @Mock
  private HoldingsClient holdingsClient;
  @Mock
  private InstanceClient instanceClient;
  @Mock
  private LocationClient locationClient;
  @Mock
  private FolioExecutionContext folioExecutionContext;
  @Mock
  private FolioModuleMetadata folioModuleMetadata;
  @InjectMocks
  private HoldingsReferenceService service;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    when(folioExecutionContext.getOkapiHeaders()).thenReturn(Map.of(TENANT, List.of("diku")));
  }

  @Test
  void getInstanceTitleById_returnsEmptyForEmptyId() {
    String result = service.getInstanceTitleById("", "tenant");
    assertThat(result).isEmpty();
  }

  @Test
  void getInstanceTitleById_throwsBulkEditExceptionOnNotFound() {
    when(instanceClient.getInstanceJsonById("notfound")).thenThrow(new NotFoundException("not found"));
    assertThatThrownBy(() -> service.getInstanceTitleById("notfound", "tenant"))
      .isInstanceOf(BulkEditException.class)
      .hasMessageContaining("Instance not found by id=notfound")
      .extracting("errorType").isEqualTo(ErrorType.WARNING);
  }

  @Test
  void getInstanceTitleById_returnsTitleWithPublication() {
    ObjectMapper mapper = new ObjectMapper();
    JsonNode instanceJson = mapper.createObjectNode()
      .put("title", "Test Title")
      .set("publication", mapper.createArrayNode()
        .add(mapper.createObjectNode()
          .put("publisher", "Pub")
          .put("dateOfPublication", "2020")));
    when(instanceClient.getInstanceJsonById("id1")).thenReturn(instanceJson);

    String result = service.getInstanceTitleById("id1", "tenant");
    assertThat(result).isEqualTo("Test Title. Pub, 2020");
  }

  @Test
  void getInstanceTitleById_returnsTitleWithPublisherOnly() {
    ObjectMapper mapper = new ObjectMapper();
    JsonNode instanceJson = mapper.createObjectNode()
      .put("title", "Test Title")
      .set("publication", mapper.createArrayNode()
        .add(mapper.createObjectNode()
          .put("publisher", "Pub")));
    when(instanceClient.getInstanceJsonById("id2")).thenReturn(instanceJson);

    String result = service.getInstanceTitleById("id2", "tenant");
    assertThat(result).isEqualTo("Test Title. Pub");
  }

  @Test
  void getInstanceTitleById_returnsTitleWithoutPublication() {
    ObjectMapper mapper = new ObjectMapper();
    JsonNode instanceJson = mapper.createObjectNode().put("title", "Test Title");
    when(instanceClient.getInstanceJsonById("id3")).thenReturn(instanceJson);

    String result = service.getInstanceTitleById("id3", "tenant");
    assertThat(result).isEqualTo("Test Title");
  }

  @Test
  void getHoldingsJsonById_returnsJson() {
    ObjectMapper mapper = new ObjectMapper();
    JsonNode holdingsJson = mapper.createObjectNode().put("id", "h1");
    when(holdingsClient.getHoldingsJsonById("h1")).thenReturn(holdingsJson);

    JsonNode result = service.getHoldingsJsonById("h1", "tenant");
    assertThat(result.get("id").asText()).isEqualTo("h1");
  }

  @Test
  void getHoldingsLocationById_returnsEmptyObjectForEmptyId() {
    JsonNode result = service.getHoldingsLocationById("", "tenant");
    assertThat(result.isObject()).isTrue();
    assertThat(result.size()).isZero();
  }

  @Test
  void getHoldingsLocationById_returnsJson() {
    ObjectMapper mapper = new ObjectMapper();
    JsonNode locationJson = mapper.createObjectNode().put("id", "loc1");
    when(locationClient.getLocationJsonById("loc1")).thenReturn(locationJson);

    JsonNode result = service.getHoldingsLocationById("loc1", "tenant");
    assertThat(result.get("id").asText()).isEqualTo("loc1");
  }
}
