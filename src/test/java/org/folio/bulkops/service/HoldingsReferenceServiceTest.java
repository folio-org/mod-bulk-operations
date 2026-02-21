package org.folio.bulkops.service;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.folio.bulkops.util.Constants.CALL_NUMBER;
import static org.folio.bulkops.util.Constants.CALL_NUMBER_PREFIX;
import static org.folio.bulkops.util.Constants.CALL_NUMBER_SUFFIX;
import static org.folio.bulkops.util.Constants.IS_ACTIVE;
import static org.folio.bulkops.util.Constants.NAME;
import static org.folio.bulkops.util.Constants.PERMANENT_LOCATION_ID;
import static org.folio.spring.integration.XOkapiHeaders.TENANT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import org.folio.bulkops.client.HoldingsStorageClient;
import org.folio.bulkops.client.InstanceClient;
import org.folio.bulkops.client.InstanceStorageClient;
import org.folio.bulkops.client.ItemClient;
import org.folio.bulkops.client.LocationClient;
import org.folio.bulkops.domain.bean.BriefInstance;
import org.folio.bulkops.domain.bean.BriefInstanceCollection;
import org.folio.bulkops.domain.bean.HoldingsRecord;
import org.folio.bulkops.domain.bean.Item;
import org.folio.bulkops.domain.bean.ItemCollection;
import org.folio.bulkops.domain.bean.StatisticalCode;
import org.folio.bulkops.domain.bean.StatisticalCodeType;
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
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

class HoldingsReferenceServiceTest {

  @Mock private HoldingsStorageClient holdingsStorageClient;
  @Mock private InstanceClient instanceClient;
  @Mock private InstanceStorageClient instanceStorageClient;
  @Mock private LocationClient locationClient;
  @Mock private FolioExecutionContext folioExecutionContext;
  @Mock private FolioModuleMetadata folioModuleMetadata;
  @Mock private ItemClient itemClient;
  @Mock private HoldingsReferenceCacheService holdingsReferenceCacheService;
  @InjectMocks private HoldingsReferenceService holdingsReferenceService;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    when(folioExecutionContext.getOkapiHeaders()).thenReturn(Map.of(TENANT, List.of("diku")));
  }

  @Test
  void getInstanceTitleById_returnsEmptyForEmptyId() {
    String result = holdingsReferenceService.getInstanceTitleById("", "tenant");
    assertThat(result).isEmpty();
  }

  @Test
  void getInstanceTitleById_throwsBulkEditExceptionOnNotFound() {
    when(instanceStorageClient.getInstanceJsonById("notfound"))
        .thenThrow(new NotFoundException("not found"));
    assertThatThrownBy(() -> holdingsReferenceService.getInstanceTitleById("notfound", "tenant"))
        .isInstanceOf(BulkEditException.class)
        .hasMessageContaining("Instance not found by id=notfound")
        .extracting("errorType")
        .isEqualTo(ErrorType.WARNING);
  }

  @Test
  void getInstanceTitleById_returnsTitleWithPublication() {
    ObjectMapper mapper = new ObjectMapper();
    JsonNode instanceJson =
        mapper
            .createObjectNode()
            .put("title", "Test Title")
            .set(
                "publication",
                mapper
                    .createArrayNode()
                    .add(
                        mapper
                            .createObjectNode()
                            .put("publisher", "Pub")
                            .put("dateOfPublication", "2020")));
    when(instanceStorageClient.getInstanceJsonById("id1")).thenReturn(instanceJson);

    String result = holdingsReferenceService.getInstanceTitleById("id1", "tenant");
    assertThat(result).isEqualTo("Test Title. Pub, 2020");
  }

  @Test
  void getInstanceTitleById_returnsTitleWithPublisherOnly() {
    ObjectMapper mapper = new ObjectMapper();
    JsonNode instanceJson =
        mapper
            .createObjectNode()
            .put("title", "Test Title")
            .set(
                "publication",
                mapper.createArrayNode().add(mapper.createObjectNode().put("publisher", "Pub")));
    when(instanceStorageClient.getInstanceJsonById("id2")).thenReturn(instanceJson);

    String result = holdingsReferenceService.getInstanceTitleById("id2", "tenant");
    assertThat(result).isEqualTo("Test Title. Pub");
  }

  @Test
  void getInstanceTitleById_returnsTitleWithoutPublication() {
    ObjectMapper mapper = new ObjectMapper();
    JsonNode instanceJson = mapper.createObjectNode().put("title", "Test Title");
    when(instanceStorageClient.getInstanceJsonById("id3")).thenReturn(instanceJson);

    String result = holdingsReferenceService.getInstanceTitleById("id3", "tenant");
    assertThat(result).isEqualTo("Test Title");
  }

  @Test
  void getHoldingsLocationById_returnsJson() {
    ObjectMapper mapper = new ObjectMapper();
    JsonNode locationJson = mapper.createObjectNode().put("id", "loc1");
    when(holdingsReferenceCacheService.getHoldingsLocationById("loc1", "tenant"))
        .thenReturn(locationJson);

    JsonNode result = holdingsReferenceService.getHoldingsLocationById("loc1", "tenant");
    assertThat(result.get("id").asString()).isEqualTo("loc1");
  }

  @Test
  void getInstanceIdByHrid_returnsIdIfFound() {
    var instance = new BriefInstance().withId("inst-123");
    var collection = new BriefInstanceCollection();
    collection.setInstances(List.of(instance));
    when(instanceClient.getByQuery(anyString())).thenReturn(collection);

    String result = holdingsReferenceService.getInstanceIdByHrid("hrid-1");
    assertThat(result).isEqualTo("inst-123");
  }

  @Test
  void getInstanceIdByHrid_throwsBulkEditExceptionIfNotFound() {
    var collection = new BriefInstanceCollection().withInstances(List.of());
    when(instanceClient.getByQuery(anyString())).thenReturn(collection);

    assertThatThrownBy(() -> holdingsReferenceService.getInstanceIdByHrid("hrid-2"))
        .isInstanceOf(BulkEditException.class)
        .hasMessageContaining("Instance not found by hrid=hrid-2")
        .extracting("errorType")
        .isEqualTo(ErrorType.WARNING);
  }

  @Test
  void getHoldingsIdByItemBarcode_returnsHoldingsIdIfFound() {
    Item item = new Item();
    item.setHoldingsRecordId("holdings-123");
    ItemCollection collection = new ItemCollection();
    collection.setItems(List.of(item));
    when(itemClient.getByQuery(anyString(), eq(1))).thenReturn(collection);

    String result = holdingsReferenceService.getHoldingsIdByItemBarcode("barcode-1");
    assertThat(result).isEqualTo("holdings-123");
  }

  @Test
  void getHoldingsIdByItemBarcode_throwsBulkEditExceptionIfNotFound() {
    ItemCollection collection = new ItemCollection();
    collection.setItems(List.of());
    when(itemClient.getByQuery(anyString(), eq(1))).thenReturn(collection);

    assertThatThrownBy(() -> holdingsReferenceService.getHoldingsIdByItemBarcode("barcode-2"))
        .isInstanceOf(BulkEditException.class)
        .hasMessageContaining("Item not found by barcode=barcode-2")
        .extracting("errorType")
        .isEqualTo(ErrorType.WARNING);
  }

  @Test
  void getInstanceTitleById_returnsTitleWithPublisherAndDate() {
    ObjectMapper mapper = new ObjectMapper();
    JsonNode publication =
        mapper
            .createObjectNode()
            .put("publisher", "Test Publisher")
            .put("dateOfPublication", "2021");
    JsonNode instanceJson =
        mapper
            .createObjectNode()
            .put("title", "Title1")
            .set("publication", mapper.createArrayNode().add(publication));
    when(instanceStorageClient.getInstanceJsonById("id-pub-date")).thenReturn(instanceJson);

    String result = holdingsReferenceService.getInstanceTitleById("id-pub-date", "tenant");
    assertThat(result).isEqualTo("Title1. Test Publisher, 2021");
  }

  @Test
  void getInstanceTitleById_returnsTitleWithPublisherOnly2() {
    ObjectMapper mapper = new ObjectMapper();
    JsonNode publication = mapper.createObjectNode().put("publisher", "Only Publisher");
    JsonNode instanceJson =
        mapper
            .createObjectNode()
            .put("title", "Title2")
            .set("publication", mapper.createArrayNode().add(publication));
    when(instanceStorageClient.getInstanceJsonById("id-pub-only")).thenReturn(instanceJson);

    String result = holdingsReferenceService.getInstanceTitleById("id-pub-only", "tenant");
    assertThat(result).isEqualTo("Title2. Only Publisher");
  }

  @Test
  void getInstanceTitleById_returnsTitleWithDateOnly() {
    ObjectMapper mapper = new ObjectMapper();
    JsonNode publication = mapper.createObjectNode().put("dateOfPublication", "2022");
    JsonNode instanceJson =
        mapper
            .createObjectNode()
            .put("title", "Title3")
            .set("publication", mapper.createArrayNode().add(publication));
    when(instanceStorageClient.getInstanceJsonById("id-date-only")).thenReturn(instanceJson);

    String result = holdingsReferenceService.getInstanceTitleById("id-date-only", "tenant");
    assertThat(result).isEqualTo("Title3. , 2022");
  }

  @Test
  void getInstanceTitleById_returnsTitleWithEmptyPublication() {
    ObjectMapper mapper = new ObjectMapper();
    JsonNode publication = mapper.createObjectNode();
    JsonNode instanceJson =
        mapper
            .createObjectNode()
            .put("title", "Title4")
            .set("publication", mapper.createArrayNode().add(publication));
    when(instanceStorageClient.getInstanceJsonById("id-empty-pub")).thenReturn(instanceJson);

    String result = holdingsReferenceService.getInstanceTitleById("id-empty-pub", "tenant");
    assertThat(result).isEqualTo("Title4");
  }

  @Test
  void getInstanceTitleById_returnsTitleWhenNoPublicationArray() {
    ObjectMapper mapper = new ObjectMapper();
    JsonNode instanceJson = mapper.createObjectNode().put("title", "Title5");
    when(instanceStorageClient.getInstanceJsonById("id-no-pub")).thenReturn(instanceJson);

    String result = holdingsReferenceService.getInstanceTitleById("id-no-pub", "tenant");
    assertThat(result).isEqualTo("Title5");
  }

  @Test
  void getInstanceTitleById_returnsTitleWhenPublicationArrayIsEmpty() {
    ObjectMapper mapper = new ObjectMapper();
    JsonNode instanceJson =
        mapper
            .createObjectNode()
            .put("title", "Title6")
            .set("publication", mapper.createArrayNode());
    when(instanceStorageClient.getInstanceJsonById("id-empty-pub-array")).thenReturn(instanceJson);

    String result = holdingsReferenceService.getInstanceTitleById("id-empty-pub-array", "tenant");
    assertThat(result).isEqualTo("Title6");
  }

  @Test
  void getInstanceTitle_returnsTitle_whenHoldingsAndInstanceExist() throws JacksonException {
    Item item = new Item().withHoldingsRecordId("holdingsId");
    HoldingsRecord holdingsRecord = new HoldingsRecord().withInstanceId("instanceId");

    when(holdingsReferenceCacheService.getHoldingsRecordById("holdingsId", "tenant"))
        .thenReturn(holdingsRecord);

    when(instanceStorageClient.getInstanceJsonById("instanceId"))
        .thenReturn(new ObjectMapper().readTree("{\"title\":\"Instance Title\"}"));

    String result =
        holdingsReferenceService.getInstanceTitleByHoldingsRecordId(
            item.getHoldingsRecordId(), "tenant");
    assertEquals("Instance Title", result);
  }

  @Test
  void getInstanceTitle_returnsEmpty_whenHoldingsNotFound() {
    Item item = new Item().withHoldingsRecordId("holdingsId");
    when(holdingsReferenceService.getHoldingsRecordById("holdingsId", "tenant")).thenReturn(null);

    String result =
        holdingsReferenceService.getInstanceTitleByHoldingsRecordId(
            item.getHoldingsRecordId(), "tenant");
    assertEquals(EMPTY, result);
  }

  @Test
  void getHoldingsData_returnsFormattedString_whenDataPresent() {
    ObjectNode holdingsJson = JsonNodeFactory.instance.objectNode();
    holdingsJson.put(PERMANENT_LOCATION_ID, "locId");
    holdingsJson.put(CALL_NUMBER_PREFIX, "PRE");
    holdingsJson.put(CALL_NUMBER, "123");
    holdingsJson.put(CALL_NUMBER_SUFFIX, "SUF");

    ObjectNode locationJson = JsonNodeFactory.instance.objectNode();
    locationJson.put(IS_ACTIVE, true);
    locationJson.put(NAME, "Main Library");
    String holdingsId = "holdingsId";
    String tenantId = "tenant";
    when(holdingsReferenceCacheService.getHoldingsJsonById(holdingsId, tenantId))
        .thenReturn(holdingsJson);
    when(holdingsReferenceCacheService.getHoldingsLocationById("locId", tenantId))
        .thenReturn(locationJson);

    String result = holdingsReferenceService.getHoldingsData(holdingsId, tenantId);
    assertEquals("Main Library > PRE 123 SUF", result);
  }

  @Test
  void getHoldingsData_returnsInactiveLocation_whenLocationIsInactive() {
    ObjectNode holdingsJson = JsonNodeFactory.instance.objectNode();
    holdingsJson.put(PERMANENT_LOCATION_ID, "locId");

    ObjectNode locationJson = JsonNodeFactory.instance.objectNode();
    locationJson.put(IS_ACTIVE, false);
    locationJson.put(NAME, "Branch");
    String holdingsId = "holdingsId";
    String tenantId = "tenant";
    when(holdingsReferenceCacheService.getHoldingsJsonById(holdingsId, tenantId))
        .thenReturn(holdingsJson);
    when(holdingsReferenceCacheService.getHoldingsLocationById("locId", tenantId))
        .thenReturn(locationJson);

    String result = holdingsReferenceService.getHoldingsData(holdingsId, tenantId);
    assertEquals("Inactive Branch > ", result);
  }

  @Test
  void getHoldingsData_returnsEmpty_whenHoldingsIdIsEmpty() {
    String result = holdingsReferenceService.getHoldingsData("", "tenant");
    assertEquals(EMPTY, result);
  }

  @Test
  void getStatisticalCodeById_returnsStatisticalCode() {
    StatisticalCode statisticalCode =
        new StatisticalCode()
            .withId("stat-id-1")
            .withName("Agriculture")
            .withCode("ABC")
            .withStatisticalCodeTypeId("stat-type-1");
    when(holdingsReferenceCacheService.getStatisticalCodeById("stat-id-1", "test"))
        .thenReturn(statisticalCode);

    StatisticalCode result = holdingsReferenceService.getStatisticalCodeById("stat-id-1", "test");
    assertEquals("Agriculture", result.getName());
    assertEquals("ABC", result.getCode());
    assertEquals("stat-id-1", result.getId());
    assertEquals("stat-type-1", result.getStatisticalCodeTypeId());
  }

  @Test
  void getStatisticalCodeByName_returnsStatisticalCode() {
    StatisticalCode statisticalCode =
        new StatisticalCode()
            .withId("stat-id-2")
            .withName("Science")
            .withCode("SCI")
            .withStatisticalCodeTypeId("stat-type-2");
    when(holdingsReferenceCacheService.getStatisticalCodeByName("Science", "tenant"))
        .thenReturn(statisticalCode);

    StatisticalCode result = holdingsReferenceService.getStatisticalCodeByName("Science", "tenant");
    assertEquals("Science", result.getName());
    assertEquals("SCI", result.getCode());
    assertEquals("stat-id-2", result.getId());
  }

  @Test
  void getStatisticalCodeTypeById_returnsStatisticalCodeType() {
    StatisticalCodeType statisticalCodeType =
        new StatisticalCodeType().withId("stat-type-id-1").withName("Subject").withSource("local");
    when(holdingsReferenceCacheService.getStatisticalCodeTypeById("stat-type-id-1", "test"))
        .thenReturn(statisticalCodeType);

    StatisticalCodeType result =
        holdingsReferenceService.getStatisticalCodeTypeById("stat-type-id-1", "test");
    assertEquals("Subject", result.getName());
    assertEquals("stat-type-id-1", result.getId());
    assertEquals("local", result.getSource());
  }
}
