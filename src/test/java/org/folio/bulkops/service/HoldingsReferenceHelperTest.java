package org.folio.bulkops.service;

import org.folio.bulkops.BaseTest;
import org.folio.bulkops.domain.bean.*;
import org.folio.bulkops.exception.NotFoundException;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.integration.XOkapiHeaders;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HoldingsReferenceHelperTest extends BaseTest {

  @Autowired
  private HoldingsReferenceHelper holdingsReferenceHelper;
  @SpyBean
  private FolioExecutionContext folioExecutionContext;

  @Test
  void testGetHoldingsType() {
    when(holdingsTypeClient.getById("id_1")).thenReturn(new HoldingsType().withName("name_1"));
    var actual = holdingsReferenceHelper.getHoldingsTypeById("id_1");
    assertEquals("name_1", actual.getName());

    when(holdingsTypeClient.getByQuery("name==\"name_2\"")).thenReturn(new HoldingsTypeCollection().withHoldingsTypes(Collections.singletonList(new HoldingsType().withId("id_2"))));
    actual = holdingsReferenceHelper.getHoldingsTypeByName("name_2");
    assertEquals("id_2", actual.getId());

    when(holdingsTypeClient.getById("id_3")).thenThrow(new NotFoundException("Not found"));
    assertThrows(NotFoundException.class, () -> holdingsReferenceHelper.getHoldingsTypeById("id_3"));

    when(holdingsTypeClient.getByQuery("name==\"name_4\"")).thenReturn(new HoldingsTypeCollection().withHoldingsTypes(Collections.emptyList()));
    assertThrows(NotFoundException.class, () -> holdingsReferenceHelper.getHoldingsTypeByName("name_4"));
  }

  @Test
  void testGetLocation() {
    HashMap<String, Collection<String>> headers = new HashMap<>();
    headers.put(XOkapiHeaders.TENANT, List.of("diku"));
    when(folioExecutionContext.getOkapiHeaders()).thenReturn(headers);
    when(folioExecutionContext.getTenantId()).thenReturn("diku");
    when(folioExecutionContext.getAllHeaders()).thenReturn(headers);
    when(locationClient.getLocationById("id_1")).thenReturn(new ItemLocation().withName("name_1"));
    var actual = holdingsReferenceHelper.getLocationById("id_1", null);
    assertEquals("name_1", actual.getName());

    when(locationClient.getByQuery("name==\"name_2\"")).thenReturn(new ItemLocationCollection().withLocations(Collections.singletonList(new ItemLocation().withId("id_2"))));
    actual = holdingsReferenceHelper.getLocationByName("name_2");
    assertEquals("id_2", actual.getId());

    when(locationClient.getLocationById("id_3")).thenThrow(new NotFoundException("Not found"));
    assertThrows(NotFoundException.class, () -> holdingsReferenceHelper.getLocationById("id_3", null));

    when(locationClient.getByQuery("name==\"name_4\"")).thenReturn(new ItemLocationCollection().withLocations(Collections.emptyList()));
    assertThrows(NotFoundException.class, () -> holdingsReferenceHelper.getLocationByName("name_4"));
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
    assertThrows(NotFoundException.class, () -> holdingsReferenceHelper.getCallNumberTypeNameById("id_3"));

    when(callNumberTypeClient.getByQuery("name==\"name_4\"")).thenReturn(new CallNumberTypeCollection().withCallNumberTypes(Collections.emptyList()));
    assertThrows(NotFoundException.class, () -> holdingsReferenceHelper.getCallNumberTypeIdByName("name_4"));
  }

  @Test
  void testGetNoteType() {
    when(holdingsNoteTypeClient.getNoteTypeById("id_1")).thenReturn(new HoldingsNoteType().withName("name_1"));
    var actual = holdingsReferenceHelper.getNoteTypeNameById("id_1");
    assertEquals("name_1", actual);

    when(holdingsNoteTypeClient.getNoteTypesByQuery("name==\"name_2\"", 1)).thenReturn(new HoldingsNoteTypeCollection().withHoldingsNoteTypes(Collections.singletonList(new HoldingsNoteType().withId("id_2"))));
    actual = holdingsReferenceHelper.getNoteTypeIdByName("name_2");
    assertEquals("id_2", actual);

    when(holdingsNoteTypeClient.getNoteTypeById("id_3")).thenThrow(new NotFoundException("Not found"));
    assertThrows(NotFoundException.class, () -> holdingsReferenceHelper.getNoteTypeNameById("id_3"));

    when(holdingsNoteTypeClient.getNoteTypesByQuery("name==\"name_4\"", 1)).thenReturn(new HoldingsNoteTypeCollection().withHoldingsNoteTypes(Collections.emptyList()));
    assertThrows(NotFoundException.class, () -> holdingsReferenceHelper.getNoteTypeIdByName("name_4"));
  }

  @Test
  void testGetIllPolicy() {
    when(illPolicyClient.getById("id_1")).thenReturn(new IllPolicy().withName("name_1"));
    var actual = holdingsReferenceHelper.getIllPolicyNameById("id_1");
    assertEquals("name_1", actual.getName());

    when(illPolicyClient.getByQuery("name==\"name_2\"")).thenReturn(new IllPolicyCollection().withIllPolicies(Collections.singletonList(new IllPolicy().withId("id_2"))));
    actual = holdingsReferenceHelper.getIllPolicyByName("name_2");
    assertEquals("id_2", actual.getId());

    when(illPolicyClient.getById("id_3")).thenThrow(new NotFoundException("Not found"));
    assertThrows(NotFoundException.class, () -> holdingsReferenceHelper.getIllPolicyNameById("id_3"));


    when(illPolicyClient.getByQuery("name==\"name_4\"")).thenReturn(new IllPolicyCollection().withIllPolicies(Collections.emptyList()));
    assertThrows(NotFoundException.class, () -> holdingsReferenceHelper.getIllPolicyByName("name_4"));
  }

  @Test
  void testGetSource() {
    when(holdingsSourceClient.getById("id_1")).thenReturn(new HoldingsRecordsSource().withName("name_1"));
    var actual = holdingsReferenceHelper.getSourceById("id_1");
    assertEquals("name_1", actual.getName());

    when(holdingsSourceClient.getByQuery("name==\"name_2\"")).thenReturn(new HoldingsRecordsSourceCollection().withHoldingsRecordsSources(Collections.singletonList(new HoldingsRecordsSource().withId("id_2"))));
    actual = holdingsReferenceHelper.getSourceByName("name_2");
    assertEquals("id_2", actual.getId());

    when(holdingsSourceClient.getById("id_3")).thenThrow(new NotFoundException("Not found"));
    assertThrows(NotFoundException.class, () -> holdingsReferenceHelper.getSourceById("id_3"));

    when(holdingsSourceClient.getByQuery("name==\"name_4\"")).thenReturn(new HoldingsRecordsSourceCollection().withHoldingsRecordsSources(Collections.emptyList()));
    assertThrows(NotFoundException.class, () -> holdingsReferenceHelper.getSourceByName("name_4"));
  }

  @Test
  void testStatisticalCode() {
    when(statisticalCodeClient.getById("id_1")).thenReturn(new StatisticalCode().withName("name_1"));
    var actual = holdingsReferenceHelper.getStatisticalCodeById("id_1");
    assertEquals("name_1", actual.getName());

    when(statisticalCodeClient.getByQuery("name==\"name_2\"")).thenReturn(new StatisticalCodeCollection().withStatisticalCodes(Collections.singletonList(new StatisticalCode().withId("id_2"))));
    actual = holdingsReferenceHelper.getStatisticalCodeByName("name_2");
    assertEquals("id_2", actual.getId());

    when(statisticalCodeClient.getById("id_3")).thenThrow(new NotFoundException("Not found"));
    assertThrows(NotFoundException.class, () -> holdingsReferenceHelper.getStatisticalCodeById("id_3"));

    when(statisticalCodeClient.getByQuery("name==\"name_4\"")).thenReturn(new StatisticalCodeCollection().withStatisticalCodes(Collections.emptyList()));
    assertThrows(NotFoundException.class, () -> holdingsReferenceHelper.getStatisticalCodeByName("name_4"));

  }
}
