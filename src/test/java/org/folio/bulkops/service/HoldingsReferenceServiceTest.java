package org.folio.bulkops.service;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

import org.folio.bulkops.BaseTest;
import org.folio.bulkops.client.CallNumberTypeClient;
import org.folio.bulkops.client.HoldingsNoteTypeClient;
import org.folio.bulkops.client.HoldingsTypeClient;
import org.folio.bulkops.client.IllPolicyClient;
import org.folio.bulkops.client.InstanceClient;
import org.folio.bulkops.client.StatisticalCodeClient;
import org.folio.bulkops.domain.bean.BriefInstance;
import org.folio.bulkops.domain.bean.CallNumberType;
import org.folio.bulkops.domain.bean.CallNumberTypeCollection;
import org.folio.bulkops.domain.bean.HoldingsNoteType;
import org.folio.bulkops.domain.bean.HoldingsNoteTypeCollection;
import org.folio.bulkops.domain.bean.HoldingsRecordsSource;
import org.folio.bulkops.domain.bean.HoldingsRecordsSourceCollection;
import org.folio.bulkops.domain.bean.HoldingsType;
import org.folio.bulkops.domain.bean.HoldingsTypeCollection;
import org.folio.bulkops.domain.bean.IllPolicy;
import org.folio.bulkops.domain.bean.IllPolicyCollection;
import org.folio.bulkops.domain.bean.ItemLocation;
import org.folio.bulkops.domain.bean.ItemLocationCollection;
import org.folio.bulkops.domain.bean.StatisticalCode;
import org.folio.bulkops.domain.bean.StatisticalCodeCollection;
import org.folio.bulkops.exception.NotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.Collections;

class HoldingsReferenceServiceTest extends BaseTest {
  @MockBean
  private InstanceClient instanceClient;
  @MockBean
  private HoldingsTypeClient holdingsTypeClient;
  @MockBean
  private CallNumberTypeClient callNumberTypeClient;
  @MockBean
  private HoldingsNoteTypeClient holdingsNoteTypeClient;
  @MockBean
  private IllPolicyClient illPolicyClient;
  @MockBean
  private StatisticalCodeClient statisticalCodeClient;
  @Autowired
  private HoldingsReferenceService holdingsReferenceService;
  @Test
  void testGetInstanceTitle() {
    when(instanceClient.getById("id")).thenReturn(new BriefInstance().withTitle("title"));
    var actual = holdingsReferenceService.getInstanceTitleById("id");
    assertEquals("title", actual);

    when(instanceClient.getById("id")).thenThrow(new NotFoundException("Not found"));
    actual = holdingsReferenceService.getInstanceTitleById("id");
    assertEquals("id", actual);

    assertEquals(EMPTY, holdingsReferenceService.getInstanceTitleById(null));
  }

  @Test
  void testGetHoldingsType() {
    when(holdingsTypeClient.getById("id")).thenReturn(new HoldingsType().withName("name"));
    var actual = holdingsReferenceService.getHoldingsTypeNameById("id");
    assertEquals("name", actual);

    when(holdingsTypeClient.getByQuery("name==\"name\"")).thenReturn(new HoldingsTypeCollection().withHoldingsTypes(Collections.singletonList(new HoldingsType().withId("id"))));
    actual = holdingsReferenceService.getHoldingsTypeIdByName("name");
    assertEquals("id", actual);

    when(holdingsTypeClient.getById("id")).thenThrow(new NotFoundException("Not found"));
    actual = holdingsReferenceService.getHoldingsTypeNameById("id");
    assertEquals("id", actual);

    when(holdingsTypeClient.getByQuery("name==\"name\"")).thenReturn(new HoldingsTypeCollection().withHoldingsTypes(Collections.emptyList()));
    actual = holdingsReferenceService.getHoldingsTypeIdByName("name");
    assertEquals("name", actual);

    assertEquals(EMPTY, holdingsReferenceService.getHoldingsTypeNameById(null));
    assertNull(holdingsReferenceService.getNoteTypeIdByName(EMPTY));
  }

  @Test
  void testGetLocation() {
    when(locationClient.getLocationById("id")).thenReturn(new ItemLocation().withName("name"));
    var actual = holdingsReferenceService.getLocationNameById("id");
    assertEquals("name", actual);

    when(locationClient.getLocationByQuery("name==\"name\"")).thenReturn(new ItemLocationCollection().withLocations(Collections.singletonList(new ItemLocation().withId("id"))));
    actual = holdingsReferenceService.getLocationIdByName("name");
    assertEquals("id", actual);

    when(locationClient.getLocationById("id")).thenThrow(new NotFoundException("Not found"));
    actual = holdingsReferenceService.getLocationNameById("id");
    assertEquals("id", actual);

    when(locationClient.getLocationByQuery("name==\"name\"")).thenReturn(new ItemLocationCollection().withLocations(Collections.emptyList()));
    actual = holdingsReferenceService.getLocationIdByName("name");
    assertEquals("name", actual);

    assertEquals(EMPTY, holdingsReferenceService.getLocationNameById(null));
    assertNull(holdingsReferenceService.getLocationIdByName(EMPTY));
  }

  @Test
  void testGetCallNumberType() {
    when(callNumberTypeClient.getById("id")).thenReturn(new CallNumberType().withName("name"));
    var actual = holdingsReferenceService.getCallNumberTypeNameById("id");
    assertEquals("name", actual);

    when(callNumberTypeClient.getByQuery("name==\"name\"")).thenReturn(new CallNumberTypeCollection().withCallNumberTypes(Collections.singletonList(new CallNumberType().withId("id"))));
    actual = holdingsReferenceService.getCallNumberTypeIdByName("name");
    assertEquals("id", actual);

    when(callNumberTypeClient.getById("id")).thenThrow(new NotFoundException("Not found"));
    actual = holdingsReferenceService.getCallNumberTypeNameById("id");
    assertEquals("id", actual);

    when(callNumberTypeClient.getByQuery("name==\"name\"")).thenReturn(new CallNumberTypeCollection().withCallNumberTypes(Collections.emptyList()));
    actual = holdingsReferenceService.getCallNumberTypeIdByName("name");
    assertEquals("name", actual);

    assertEquals(EMPTY, holdingsReferenceService.getCallNumberTypeNameById(null));
    assertNull(holdingsReferenceService.getCallNumberTypeIdByName(EMPTY));
  }

  @Test
  void testGetNoteType() {
    when(holdingsNoteTypeClient.getById("id")).thenReturn(new HoldingsNoteType().withName("name"));
    var actual = holdingsReferenceService.getNoteTypeNameById("id");
    assertEquals("name", actual);

    when(holdingsNoteTypeClient.getByQuery("name==\"name\"")).thenReturn(new HoldingsNoteTypeCollection().withHoldingsNoteTypes(Collections.singletonList(new HoldingsNoteType().withId("id"))));
    actual = holdingsReferenceService.getNoteTypeIdByName("name");
    assertEquals("id", actual);

    when(holdingsNoteTypeClient.getById("id")).thenThrow(new NotFoundException("Not found"));
    actual = holdingsReferenceService.getNoteTypeNameById("id");
    assertEquals("id", actual);

    when(holdingsNoteTypeClient.getByQuery("name==\"name\"")).thenReturn(new HoldingsNoteTypeCollection().withHoldingsNoteTypes(Collections.emptyList()));
    actual = holdingsReferenceService.getNoteTypeIdByName("name");
    assertEquals("name", actual);

    assertEquals(EMPTY, holdingsReferenceService.getNoteTypeNameById(null));
    assertNull(holdingsReferenceService.getNoteTypeIdByName(EMPTY));
  }

  @Test
  void testGetIllPolicy() {
    when(illPolicyClient.getById("id")).thenReturn(new IllPolicy().withName("name"));
    var actual = holdingsReferenceService.getIllPolicyNameById("id");
    assertEquals("name", actual);

    when(illPolicyClient.getByQuery("name==\"name\"")).thenReturn(new IllPolicyCollection().withIllPolicies(Collections.singletonList(new IllPolicy().withId("id"))));
    actual = holdingsReferenceService.getIllPolicyIdByName("name");
    assertEquals("id", actual);

    when(illPolicyClient.getById("id")).thenThrow(new NotFoundException("Not found"));
    actual = holdingsReferenceService.getIllPolicyNameById("id");
    assertEquals("id", actual);

    when(illPolicyClient.getByQuery("name==\"name\"")).thenReturn(new IllPolicyCollection().withIllPolicies(Collections.emptyList()));
    actual = holdingsReferenceService.getIllPolicyIdByName("name");
    assertEquals("name", actual);

    assertEquals(EMPTY, holdingsReferenceService.getIllPolicyNameById(null));
    assertNull(holdingsReferenceService.getIllPolicyIdByName(EMPTY));
  }

  @Test
  void testGetSource() {
    when(holdingsSourceClient.getById("id")).thenReturn(new HoldingsRecordsSource().withName("name"));
    var actual = holdingsReferenceService.getSourceNameById("id");
    assertEquals("name", actual);

    when(holdingsSourceClient.getByQuery("name==\"name\"")).thenReturn(new HoldingsRecordsSourceCollection().withHoldingsRecordsSources(Collections.singletonList(new HoldingsRecordsSource().withId("id"))));
    actual = holdingsReferenceService.getSourceIdByName("name");
    assertEquals("id", actual);

    when(holdingsSourceClient.getById("id")).thenThrow(new NotFoundException("Not found"));
    actual = holdingsReferenceService.getSourceNameById("id");
    assertEquals("id", actual);

    when(holdingsSourceClient.getByQuery("name==\"name\"")).thenReturn(new HoldingsRecordsSourceCollection().withHoldingsRecordsSources(Collections.emptyList()));
    actual = holdingsReferenceService.getSourceIdByName("name");
    assertEquals("name", actual);

    assertEquals(EMPTY, holdingsReferenceService.getSourceNameById(null));
  }

  @Test
  void testStatisticalCode() {
    when(statisticalCodeClient.getById("id")).thenReturn(new StatisticalCode().withName("name"));
    var actual = holdingsReferenceService.getStatisticalCodeNameById("id");
    assertEquals("name", actual);

    when(statisticalCodeClient.getByQuery("name==\"name\"")).thenReturn(new StatisticalCodeCollection().withStatisticalCodes(Collections.singletonList(new StatisticalCode().withId("id"))));
    actual = holdingsReferenceService.getStatisticalCodeIdByName("name");
    assertEquals("id", actual);

    when(statisticalCodeClient.getById("id")).thenThrow(new NotFoundException("Not found"));
    actual = holdingsReferenceService.getStatisticalCodeNameById("id");
    assertEquals("id", actual);

    when(statisticalCodeClient.getByQuery("name==\"name\"")).thenReturn(new StatisticalCodeCollection().withStatisticalCodes(Collections.emptyList()));
    actual = holdingsReferenceService.getStatisticalCodeIdByName("name");
    assertEquals("name", actual);

    assertEquals(EMPTY, holdingsReferenceService.getStatisticalCodeNameById(null));
  }
}
