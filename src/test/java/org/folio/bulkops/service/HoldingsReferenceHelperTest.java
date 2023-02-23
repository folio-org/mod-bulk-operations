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
class HoldingsReferenceHelperTest extends BaseTest {

  @Autowired
  private HoldingsReferenceHelper holdingsReferenceHelper;

  @Test
  void testGetInstanceTitle() {
    when(instanceClient.getById("id_1")).thenReturn(new BriefInstance().withTitle("title_1"));
    var actual = holdingsReferenceHelper.getInstanceTitleById("id_1");
    assertEquals("title_1", actual);

    when(instanceClient.getById("id_2")).thenThrow(new NotFoundException("Not found"));
    actual = holdingsReferenceHelper.getInstanceTitleById("id_2");
    assertEquals("id_2", actual);

    assertEquals(EMPTY, holdingsReferenceHelper.getInstanceTitleById(null));
  }

  @Test
  void testGetHoldingsType() {
    when(holdingsTypeClient.getById("id_1")).thenReturn(new HoldingsType().withName("name_1"));
    var actual = holdingsReferenceHelper.getHoldingsTypeNameById("id_1");
    assertEquals("name_1", actual);

    when(holdingsTypeClient.getByQuery("name==\"name_2\"")).thenReturn(new HoldingsTypeCollection().withHoldingsTypes(Collections.singletonList(new HoldingsType().withId("id_2"))));
    actual = holdingsReferenceHelper.getHoldingsTypeIdByName("name_2");
    assertEquals("id_2", actual);

    when(holdingsTypeClient.getById("id_3")).thenThrow(new NotFoundException("Not found"));
    actual = holdingsReferenceHelper.getHoldingsTypeNameById("id_3");
    assertEquals("id_3", actual);

    when(holdingsTypeClient.getByQuery("name==\"name_4\"")).thenReturn(new HoldingsTypeCollection().withHoldingsTypes(Collections.emptyList()));
    actual = holdingsReferenceHelper.getHoldingsTypeIdByName("name_4");
    assertEquals("name_4", actual);

    assertEquals(EMPTY, holdingsReferenceHelper.getHoldingsTypeNameById(null));
    assertNull(holdingsReferenceHelper.getNoteTypeIdByName(EMPTY));
  }

  @Test
  void testGetLocation() {
    when(locationClient.getLocationById("id_1")).thenReturn(new ItemLocation().withName("name_1"));
    var actual = holdingsReferenceHelper.getLocationNameById("id_1");
    assertEquals("name_1", actual);

    when(locationClient.getLocationByQuery("name==\"name_2\"")).thenReturn(new ItemLocationCollection().withLocations(Collections.singletonList(new ItemLocation().withId("id_2"))));
    actual = holdingsReferenceHelper.getLocationIdByName("name_2");
    assertEquals("id_2", actual);

    when(locationClient.getLocationById("id_3")).thenThrow(new NotFoundException("Not found"));
    actual = holdingsReferenceHelper.getLocationNameById("id_3");
    assertEquals("id_3", actual);

    when(locationClient.getLocationByQuery("name==\"name_4\"")).thenReturn(new ItemLocationCollection().withLocations(Collections.emptyList()));
    actual = holdingsReferenceHelper.getLocationIdByName("name_4");
    assertEquals("name_4", actual);

    assertEquals(EMPTY, holdingsReferenceHelper.getLocationNameById(null));
    assertNull(holdingsReferenceHelper.getLocationIdByName(EMPTY));
  }

  @Test
  void testGetCallNumberType() {
    when(callNumberTypeClient.getById("id_1")).thenReturn(new CallNumberType().withName("name_1"));
    var actual = holdingsReferenceHelper.getCallNumberTypeNameById("id_1");
    assertEquals("name_1", actual);

    when(callNumberTypeClient.getByQuery("name==\"name_2\"")).thenReturn(new CallNumberTypeCollection().withCallNumberTypes(Collections.singletonList(new CallNumberType().withId("id_2"))));
    actual = holdingsReferenceHelper.getCallNumberTypeIdByName("name_2");
    assertEquals("id_2", actual);

    when(callNumberTypeClient.getById("id_3")).thenThrow(new NotFoundException("Not found"));
    actual = holdingsReferenceHelper.getCallNumberTypeNameById("id_3");
    assertEquals("id_3", actual);

    when(callNumberTypeClient.getByQuery("name==\"name_4\"")).thenReturn(new CallNumberTypeCollection().withCallNumberTypes(Collections.emptyList()));
    actual = holdingsReferenceHelper.getCallNumberTypeIdByName("name_4");
    assertEquals("name_4", actual);

    assertEquals(EMPTY, holdingsReferenceHelper.getCallNumberTypeNameById(null));
    assertNull(holdingsReferenceHelper.getCallNumberTypeIdByName(EMPTY));
  }

  @Test
  void testGetNoteType() {
    when(holdingsNoteTypeClient.getById("id_1")).thenReturn(new HoldingsNoteType().withName("name_1"));
    var actual = holdingsReferenceHelper.getNoteTypeNameById("id_1");
    assertEquals("name_1", actual);

    when(holdingsNoteTypeClient.getByQuery("name==\"name_2\"")).thenReturn(new HoldingsNoteTypeCollection().withHoldingsNoteTypes(Collections.singletonList(new HoldingsNoteType().withId("id_2"))));
    actual = holdingsReferenceHelper.getNoteTypeIdByName("name_2");
    assertEquals("id_2", actual);

    when(holdingsNoteTypeClient.getById("id_3")).thenThrow(new NotFoundException("Not found"));
    actual = holdingsReferenceHelper.getNoteTypeNameById("id_3");
    assertEquals("id_3", actual);

    when(holdingsNoteTypeClient.getByQuery("name==\"name_4\"")).thenReturn(new HoldingsNoteTypeCollection().withHoldingsNoteTypes(Collections.emptyList()));
    actual = holdingsReferenceHelper.getNoteTypeIdByName("name_4");
    assertEquals("name_4", actual);

    assertEquals(EMPTY, holdingsReferenceHelper.getNoteTypeNameById(null));
    assertNull(holdingsReferenceHelper.getNoteTypeIdByName(EMPTY));
  }

  @Test
  void testGetIllPolicy() {
    when(illPolicyClient.getById("id_1")).thenReturn(new IllPolicy().withName("name_1"));
    var actual = holdingsReferenceHelper.getIllPolicyNameById("id_1");
    assertEquals("name_1", actual);

    when(illPolicyClient.getByQuery("name==\"name_2\"")).thenReturn(new IllPolicyCollection().withIllPolicies(Collections.singletonList(new IllPolicy().withId("id_2"))));
    actual = holdingsReferenceHelper.getIllPolicyIdByName("name_2");
    assertEquals("id_2", actual);

    when(illPolicyClient.getById("id_3")).thenThrow(new NotFoundException("Not found"));
    actual = holdingsReferenceHelper.getIllPolicyNameById("id_3");
    assertEquals("id_3", actual);

    when(illPolicyClient.getByQuery("name==\"name_4\"")).thenReturn(new IllPolicyCollection().withIllPolicies(Collections.emptyList()));
    actual = holdingsReferenceHelper.getIllPolicyIdByName("name_4");
    assertEquals("name_4", actual);

    assertEquals(EMPTY, holdingsReferenceHelper.getIllPolicyNameById(null));
    assertNull(holdingsReferenceHelper.getIllPolicyIdByName(EMPTY));
  }

  @Test
  void testGetSource() {
    when(holdingsSourceClient.getById("id_1")).thenReturn(new HoldingsRecordsSource().withName("name_1"));
    var actual = holdingsReferenceHelper.getSourceNameById("id_1");
    assertEquals("name_1", actual);

    when(holdingsSourceClient.getByQuery("name==\"name_2\"")).thenReturn(new HoldingsRecordsSourceCollection().withHoldingsRecordsSources(Collections.singletonList(new HoldingsRecordsSource().withId("id_2"))));
    actual = holdingsReferenceHelper.getSourceIdByName("name_2");
    assertEquals("id_2", actual);

    when(holdingsSourceClient.getById("id_3")).thenThrow(new NotFoundException("Not found"));
    actual = holdingsReferenceHelper.getSourceNameById("id_3");
    assertEquals("id_3", actual);

    when(holdingsSourceClient.getByQuery("name==\"name_4\"")).thenReturn(new HoldingsRecordsSourceCollection().withHoldingsRecordsSources(Collections.emptyList()));
    actual = holdingsReferenceHelper.getSourceIdByName("name_4");
    assertEquals("name_4", actual);

    assertEquals(EMPTY, holdingsReferenceHelper.getSourceNameById(null));
  }

  @Test
  void testStatisticalCode() {
    when(statisticalCodeClient.getById("id_1")).thenReturn(new StatisticalCode().withName("name_1"));
    var actual = holdingsReferenceHelper.getStatisticalCodeNameById("id_1");
    assertEquals("name_1", actual);

    when(statisticalCodeClient.getByQuery("name==\"name_2\"")).thenReturn(new StatisticalCodeCollection().withStatisticalCodes(Collections.singletonList(new StatisticalCode().withId("id_2"))));
    actual = holdingsReferenceHelper.getStatisticalCodeIdByName("name_2");
    assertEquals("id_2", actual);

    when(statisticalCodeClient.getById("id_3")).thenThrow(new NotFoundException("Not found"));
    actual = holdingsReferenceHelper.getStatisticalCodeNameById("id_3");
    assertEquals("id_3", actual);

    when(statisticalCodeClient.getByQuery("name==\"name_4\"")).thenReturn(new StatisticalCodeCollection().withStatisticalCodes(Collections.emptyList()));
    actual = holdingsReferenceHelper.getStatisticalCodeIdByName("name_4");
    assertEquals("name_4", actual);

    assertEquals(EMPTY, holdingsReferenceHelper.getStatisticalCodeNameById(null));
  }
}
