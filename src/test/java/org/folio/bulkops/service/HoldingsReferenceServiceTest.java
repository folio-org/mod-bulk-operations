package org.folio.bulkops.service;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

import org.folio.bulkops.BaseTest;
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
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collections;

@ExtendWith(MockitoExtension.class)
class HoldingsReferenceServiceTest extends BaseTest {

  @Autowired
  private HoldingsReferenceService holdingsReferenceService;

  @Test
  void testGetInstanceTitle() {
    when(instanceClient.getById("id_1")).thenReturn(new BriefInstance().withTitle("title_1"));
    var actual = holdingsReferenceService.getInstanceTitleById("id_1");
    assertEquals("title_1", actual);

    when(instanceClient.getById("id_2")).thenThrow(new NotFoundException("Not found"));
    actual = holdingsReferenceService.getInstanceTitleById("id_2");
    assertEquals("id_2", actual);

    assertEquals(EMPTY, holdingsReferenceService.getInstanceTitleById(null));
  }

  @Test
  void testGetHoldingsType() {
    when(holdingsTypeClient.getById("id_1")).thenReturn(new HoldingsType().withName("name_1"));
    var actual = holdingsReferenceService.getHoldingsTypeNameById("id_1");
    assertEquals("name_1", actual);

    when(holdingsTypeClient.getByQuery("name==\"name_2\"")).thenReturn(new HoldingsTypeCollection().withHoldingsTypes(Collections.singletonList(new HoldingsType().withId("id_2"))));
    actual = holdingsReferenceService.getHoldingsTypeIdByName("name_2");
    assertEquals("id_2", actual);

    when(holdingsTypeClient.getById("id_3")).thenThrow(new NotFoundException("Not found"));
    actual = holdingsReferenceService.getHoldingsTypeNameById("id_3");
    assertEquals("id_3", actual);

    when(holdingsTypeClient.getByQuery("name==\"name_4\"")).thenReturn(new HoldingsTypeCollection().withHoldingsTypes(Collections.emptyList()));
    actual = holdingsReferenceService.getHoldingsTypeIdByName("name_4");
    assertEquals("name_4", actual);

    assertEquals(EMPTY, holdingsReferenceService.getHoldingsTypeNameById(null));
    assertNull(holdingsReferenceService.getNoteTypeIdByName(EMPTY));
  }

  @Test
  void testGetLocation() {
    when(locationClient.getLocationById("id_1")).thenReturn(new ItemLocation().withName("name_1"));
    var actual = holdingsReferenceService.getLocationNameById("id_1");
    assertEquals("name_1", actual);

    when(locationClient.getLocationByQuery("name==\"name_2\"")).thenReturn(new ItemLocationCollection().withLocations(Collections.singletonList(new ItemLocation().withId("id_2"))));
    actual = holdingsReferenceService.getLocationIdByName("name_2");
    assertEquals("id_2", actual);

    when(locationClient.getLocationById("id_3")).thenThrow(new NotFoundException("Not found"));
    actual = holdingsReferenceService.getLocationNameById("id_3");
    assertEquals("id_3", actual);

    when(locationClient.getLocationByQuery("name==\"name_4\"")).thenReturn(new ItemLocationCollection().withLocations(Collections.emptyList()));
    actual = holdingsReferenceService.getLocationIdByName("name_4");
    assertEquals("name_4", actual);

    assertEquals(EMPTY, holdingsReferenceService.getLocationNameById(null));
    assertNull(holdingsReferenceService.getLocationIdByName(EMPTY));
  }

  @Test
  void testGetCallNumberType() {
    when(callNumberTypeClient.getById("id_1")).thenReturn(new CallNumberType().withName("name_1"));
    var actual = holdingsReferenceService.getCallNumberTypeNameById("id_1");
    assertEquals("name_1", actual);

    when(callNumberTypeClient.getByQuery("name==\"name_2\"")).thenReturn(new CallNumberTypeCollection().withCallNumberTypes(Collections.singletonList(new CallNumberType().withId("id_2"))));
    actual = holdingsReferenceService.getCallNumberTypeIdByName("name_2");
    assertEquals("id_2", actual);

    when(callNumberTypeClient.getById("id_3")).thenThrow(new NotFoundException("Not found"));
    actual = holdingsReferenceService.getCallNumberTypeNameById("id_3");
    assertEquals("id_3", actual);

    when(callNumberTypeClient.getByQuery("name==\"name_4\"")).thenReturn(new CallNumberTypeCollection().withCallNumberTypes(Collections.emptyList()));
    actual = holdingsReferenceService.getCallNumberTypeIdByName("name_4");
    assertEquals("name_4", actual);

    assertEquals(EMPTY, holdingsReferenceService.getCallNumberTypeNameById(null));
    assertNull(holdingsReferenceService.getCallNumberTypeIdByName(EMPTY));
  }

  @Test
  void testGetNoteType() {
    when(holdingsNoteTypeClient.getById("id_1")).thenReturn(new HoldingsNoteType().withName("name_1"));
    var actual = holdingsReferenceService.getNoteTypeNameById("id_1");
    assertEquals("name_1", actual);

    when(holdingsNoteTypeClient.getByQuery("name==\"name_2\"")).thenReturn(new HoldingsNoteTypeCollection().withHoldingsNoteTypes(Collections.singletonList(new HoldingsNoteType().withId("id_2"))));
    actual = holdingsReferenceService.getNoteTypeIdByName("name_2");
    assertEquals("id_2", actual);

    when(holdingsNoteTypeClient.getById("id_3")).thenThrow(new NotFoundException("Not found"));
    actual = holdingsReferenceService.getNoteTypeNameById("id_3");
    assertEquals("id_3", actual);

    when(holdingsNoteTypeClient.getByQuery("name==\"name_4\"")).thenReturn(new HoldingsNoteTypeCollection().withHoldingsNoteTypes(Collections.emptyList()));
    actual = holdingsReferenceService.getNoteTypeIdByName("name_4");
    assertEquals("name_4", actual);

    assertEquals(EMPTY, holdingsReferenceService.getNoteTypeNameById(null));
    assertNull(holdingsReferenceService.getNoteTypeIdByName(EMPTY));
  }

  @Test
  void testGetIllPolicy() {
    when(illPolicyClient.getById("id_1")).thenReturn(new IllPolicy().withName("name_1"));
    var actual = holdingsReferenceService.getIllPolicyNameById("id_1");
    assertEquals("name_1", actual);

    when(illPolicyClient.getByQuery("name==\"name_2\"")).thenReturn(new IllPolicyCollection().withIllPolicies(Collections.singletonList(new IllPolicy().withId("id_2"))));
    actual = holdingsReferenceService.getIllPolicyIdByName("name_2");
    assertEquals("id_2", actual);

    when(illPolicyClient.getById("id_3")).thenThrow(new NotFoundException("Not found"));
    actual = holdingsReferenceService.getIllPolicyNameById("id_3");
    assertEquals("id_3", actual);

    when(illPolicyClient.getByQuery("name==\"name_4\"")).thenReturn(new IllPolicyCollection().withIllPolicies(Collections.emptyList()));
    actual = holdingsReferenceService.getIllPolicyIdByName("name_4");
    assertEquals("name_4", actual);

    assertEquals(EMPTY, holdingsReferenceService.getIllPolicyNameById(null));
    assertNull(holdingsReferenceService.getIllPolicyIdByName(EMPTY));
  }

  @Test
  void testGetSource() {
    when(holdingsSourceClient.getById("id_1")).thenReturn(new HoldingsRecordsSource().withName("name_1"));
    var actual = holdingsReferenceService.getSourceNameById("id_1");
    assertEquals("name_1", actual);

    when(holdingsSourceClient.getByQuery("name==\"name_2\"")).thenReturn(new HoldingsRecordsSourceCollection().withHoldingsRecordsSources(Collections.singletonList(new HoldingsRecordsSource().withId("id_2"))));
    actual = holdingsReferenceService.getSourceIdByName("name_2");
    assertEquals("id_2", actual);

    when(holdingsSourceClient.getById("id_3")).thenThrow(new NotFoundException("Not found"));
    actual = holdingsReferenceService.getSourceNameById("id_3");
    assertEquals("id_3", actual);

    when(holdingsSourceClient.getByQuery("name==\"name_4\"")).thenReturn(new HoldingsRecordsSourceCollection().withHoldingsRecordsSources(Collections.emptyList()));
    actual = holdingsReferenceService.getSourceIdByName("name_4");
    assertEquals(EMPTY, actual);

    assertEquals(EMPTY, holdingsReferenceService.getSourceNameById(null));
  }

  @Test
  void testStatisticalCode() {
    when(statisticalCodeClient.getById("id_1")).thenReturn(new StatisticalCode().withName("name_1"));
    var actual = holdingsReferenceService.getStatisticalCodeNameById("id_1");
    assertEquals("name_1", actual);

    when(statisticalCodeClient.getByQuery("name==\"name_2\"")).thenReturn(new StatisticalCodeCollection().withStatisticalCodes(Collections.singletonList(new StatisticalCode().withId("id_2"))));
    actual = holdingsReferenceService.getStatisticalCodeIdByName("name_2");
    assertEquals("id_2", actual);

    when(statisticalCodeClient.getById("id_3")).thenThrow(new NotFoundException("Not found"));
    actual = holdingsReferenceService.getStatisticalCodeNameById("id_3");
    assertEquals("id_3", actual);

    when(statisticalCodeClient.getByQuery("name==\"name_4\"")).thenReturn(new StatisticalCodeCollection().withStatisticalCodes(Collections.emptyList()));
    actual = holdingsReferenceService.getStatisticalCodeIdByName("name_4");
    assertEquals("name_4", actual);

    assertEquals(EMPTY, holdingsReferenceService.getStatisticalCodeNameById(null));
  }
}
