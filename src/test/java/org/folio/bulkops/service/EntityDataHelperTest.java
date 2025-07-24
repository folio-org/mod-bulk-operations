package org.folio.bulkops.service;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.folio.bulkops.util.Constants.CALL_NUMBER;
import static org.folio.bulkops.util.Constants.CALL_NUMBER_PREFIX;
import static org.folio.bulkops.util.Constants.CALL_NUMBER_SUFFIX;
import static org.folio.bulkops.util.Constants.IS_ACTIVE;
import static org.folio.bulkops.util.Constants.NAME;
import static org.folio.bulkops.util.Constants.PERMANENT_LOCATION_ID;
import static org.folio.spring.integration.XOkapiHeaders.TENANT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.folio.bulkops.domain.bean.HoldingsRecord;
import org.folio.bulkops.domain.bean.Item;
import org.folio.spring.FolioExecutionContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import java.util.List;
import java.util.Map;

class EntityDataHelperTest {

  @Mock
  private HoldingsReferenceService holdingsReferenceService;
  @Mock
  private FolioExecutionContext folioExecutionContext;

  @InjectMocks
  private EntityDataHelper entityDataHelper;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    when(folioExecutionContext.getOkapiHeaders()).thenReturn(Map.of(TENANT, List.of("diku")));
  }

  @Test
  void getInstanceTitle_returnsTitle_whenHoldingsAndInstanceExist() {
    Item item = new Item().withHoldingsRecordId("holdingsId");
    HoldingsRecord holdingsRecord = new HoldingsRecord().withInstanceId("instanceId");

    when(holdingsReferenceService.getHoldingsRecordById("holdingsId", "tenant")).thenReturn(holdingsRecord);
    when(holdingsReferenceService.getInstanceTitleById("instanceId", "tenant")).thenReturn("Instance Title");

    String result = entityDataHelper.getInstanceTitle(item.getHoldingsRecordId(), "tenant");
    assertEquals("Instance Title", result);
  }

  @Test
  void getInstanceTitle_returnsEmpty_whenHoldingsNotFound() {
    Item item = new Item().withHoldingsRecordId("holdingsId");
    when(holdingsReferenceService.getHoldingsRecordById("holdingsId", "tenant")).thenReturn(null);

    String result = entityDataHelper.getInstanceTitle(item.getHoldingsRecordId(), "tenant");
    assertEquals(EMPTY, result);
  }

  @Test
  void getHoldingsData_returnsFormattedString_whenDataPresent() {
    String holdingsId = "holdingsId";
    String tenantId = "tenant";
    ObjectNode holdingsJson = JsonNodeFactory.instance.objectNode();
    holdingsJson.put(PERMANENT_LOCATION_ID, "locId");
    holdingsJson.put(CALL_NUMBER_PREFIX, "PRE");
    holdingsJson.put(CALL_NUMBER, "123");
    holdingsJson.put(CALL_NUMBER_SUFFIX, "SUF");

    ObjectNode locationJson = JsonNodeFactory.instance.objectNode();
    locationJson.put(IS_ACTIVE, true);
    locationJson.put(NAME, "Main Library");

    when(holdingsReferenceService.getHoldingsJsonById(holdingsId, tenantId)).thenReturn(holdingsJson);
    when(holdingsReferenceService.getHoldingsLocationById("locId", tenantId)).thenReturn(locationJson);

    String result = entityDataHelper.getHoldingsData(holdingsId, tenantId);
    assertEquals("Main Library > PRE 123 SUF", result);
  }

  @Test
  void getHoldingsData_returnsInactiveLocation_whenLocationIsInactive() {
    String holdingsId = "holdingsId";
    String tenantId = "tenant";
    ObjectNode holdingsJson = JsonNodeFactory.instance.objectNode();
    holdingsJson.put(PERMANENT_LOCATION_ID, "locId");

    ObjectNode locationJson = JsonNodeFactory.instance.objectNode();
    locationJson.put(IS_ACTIVE, false);
    locationJson.put(NAME, "Branch");

    when(holdingsReferenceService.getHoldingsJsonById(holdingsId, tenantId)).thenReturn(holdingsJson);
    when(holdingsReferenceService.getHoldingsLocationById("locId", tenantId)).thenReturn(locationJson);

    String result = entityDataHelper.getHoldingsData(holdingsId, tenantId);
    assertEquals("Inactive Branch > ", result);
  }

  @Test
  void getHoldingsData_returnsEmpty_whenHoldingsIdIsEmpty() {
    String result = entityDataHelper.getHoldingsData("", "tenant");
    assertEquals(EMPTY, result);
  }
}
