package org.folio.bulkops.service;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import org.folio.bulkops.BaseTest;
import org.folio.bulkops.client.CallNumberTypeClient;
import org.folio.bulkops.client.DamagedStatusClient;
import org.folio.bulkops.client.ItemNoteTypeClient;
import org.folio.bulkops.client.MaterialTypeClient;
import org.folio.bulkops.client.ServicePointClient;
import org.folio.bulkops.client.StatisticalCodeClient;
import org.folio.bulkops.domain.bean.CallNumberType;
import org.folio.bulkops.domain.bean.DamagedStatus;
import org.folio.bulkops.domain.bean.DamagedStatusCollection;
import org.folio.bulkops.domain.bean.ItemLocation;
import org.folio.bulkops.domain.bean.ItemLocationCollection;
import org.folio.bulkops.domain.bean.LoanType;
import org.folio.bulkops.domain.bean.LoanTypeCollection;
import org.folio.bulkops.domain.bean.MaterialType;
import org.folio.bulkops.domain.bean.MaterialTypeCollection;
import org.folio.bulkops.domain.bean.NoteType;
import org.folio.bulkops.domain.bean.NoteTypeCollection;
import org.folio.bulkops.domain.bean.ServicePoint;
import org.folio.bulkops.domain.bean.ServicePoints;
import org.folio.bulkops.domain.bean.StatisticalCode;
import org.folio.bulkops.domain.bean.StatisticalCodeCollection;
import org.folio.bulkops.domain.bean.User;
import org.folio.bulkops.domain.bean.UserCollection;
import org.folio.spring.exception.NotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.Collections;

class ItemReferenceServiceTest extends BaseTest {
  @MockBean
  private CallNumberTypeClient callNumberTypeClient;
  @MockBean
  private DamagedStatusClient damagedStatusClient;
  @MockBean
  private ItemNoteTypeClient itemNoteTypeClient;
  @MockBean
  private ServicePointClient servicePointClient;
  @MockBean
  private StatisticalCodeClient statisticalCodeClient;
  @MockBean
  private MaterialTypeClient materialTypeClient;
  @Autowired
  private ItemReferenceService itemReferenceService;

  @Test
  void testGetCallNumberTypeNameById() {
    when(callNumberTypeClient.getById("id_1")).thenReturn(new CallNumberType().withName("name_1"));
    var actual = itemReferenceService.getCallNumberTypeNameById("id_1");
    assertEquals("name_1", actual);

    when(callNumberTypeClient.getById("id_2")).thenThrow(new NotFoundException("Not found"));
    assertThrows(NotFoundException.class, () -> itemReferenceService.getCallNumberTypeNameById("id_2"));

    assertEquals(EMPTY, itemReferenceService.getCallNumberTypeNameById(null));
  }

  @Test
  void testGetDamagedStatus() {
    when(damagedStatusClient.getById("id_1")).thenReturn(new DamagedStatus().withName("name_1"));
    var actual = itemReferenceService.getDamagedStatusNameById("id_1");
    assertEquals("name_1", actual);

    when(damagedStatusClient.getByQuery("name==\"name_2\"")).thenReturn(new DamagedStatusCollection().withItemDamageStatuses(Collections.singletonList(new DamagedStatus().withId("id_2"))));
    actual = itemReferenceService.getDamagedStatusIdByName("name_2");
    assertEquals("id_2", actual);

    when(damagedStatusClient.getById("id_3")).thenThrow(new NotFoundException("Not found"));
    assertThrows(NotFoundException.class, () -> itemReferenceService.getDamagedStatusNameById("id_3"));

    when(damagedStatusClient.getByQuery("name==\"name_4\"")).thenReturn(new DamagedStatusCollection().withItemDamageStatuses(Collections.emptyList()));
    actual = itemReferenceService.getDamagedStatusIdByName("name_4");
    assertEquals("name_4", actual);

    assertEquals(EMPTY, itemReferenceService.getDamagedStatusNameById(null));
    assertNull(itemReferenceService.getDamagedStatusIdByName(EMPTY));
  }

  @Test
  void testGetNoteType() {
    when(itemNoteTypeClient.getById("id_1")).thenReturn(new NoteType().withName("name_1"));
    var actual = itemReferenceService.getNoteTypeNameById("id_1");
    assertEquals("name_1", actual);

    when(itemNoteTypeClient.getByQuery("name==\"name_2\"")).thenReturn(new NoteTypeCollection().withItemNoteTypes(Collections.singletonList(new NoteType().withId("id_2"))));
    actual = itemReferenceService.getNoteTypeIdByName("name_2");
    assertEquals("id_2", actual);

    when(itemNoteTypeClient.getById("id_3")).thenThrow(new NotFoundException("Not found"));
    assertThrows(NotFoundException.class, () -> itemReferenceService.getNoteTypeNameById("id_3"));

    when(itemNoteTypeClient.getByQuery("name==\"name_4\"")).thenReturn(new NoteTypeCollection().withItemNoteTypes(Collections.emptyList()));
    actual = itemReferenceService.getNoteTypeIdByName("name_4");
    assertEquals("name_4", actual);

    assertEquals(EMPTY, itemReferenceService.getNoteTypeNameById(null));
    assertNull(itemReferenceService.getNoteTypeIdByName(EMPTY));
  }

  @Test
  void testGetServicePoint() {
    when(servicePointClient.getById("id_1")).thenReturn(new ServicePoint().withName("name_1"));
    var actual = itemReferenceService.getServicePointNameById("id_1");
    assertEquals("name_1", actual);

    when(servicePointClient.get("name==\"name_2\"", 1L)).thenReturn(new ServicePoints().withServicepoints(Collections.singletonList(new ServicePoint().withId("id_2"))));
    actual = itemReferenceService.getServicePointIdByName("name_2");
    assertEquals("id_2", actual);

    when(servicePointClient.getById("id_3")).thenThrow(new NotFoundException("Not found"));
    assertThrows(NotFoundException.class, () -> itemReferenceService.getServicePointNameById("id_3"));

    when(servicePointClient.get("name==\"name_4\"", 1L)).thenReturn(new ServicePoints().withServicepoints(Collections.emptyList()));
    actual = itemReferenceService.getServicePointIdByName("name_4");
    assertEquals("name_4", actual);

    assertEquals(EMPTY, itemReferenceService.getServicePointNameById(null));
    assertNull(itemReferenceService.getServicePointIdByName(EMPTY));
  }

  @Test
  void testGetStatisticalCode() {
    when(statisticalCodeClient.getById("id_1")).thenReturn(new StatisticalCode().withCode("code_1"));
    var actual = itemReferenceService.getStatisticalCodeById("id_1");
    assertEquals("code_1", actual);

    when(statisticalCodeClient.getByQuery("code==\"code_2\"")).thenReturn(new StatisticalCodeCollection().withStatisticalCodes(Collections.singletonList(new StatisticalCode().withId("id_2"))));
    actual = itemReferenceService.getStatisticalCodeIdByCode("code_2");
    assertEquals("id_2", actual);

    when(statisticalCodeClient.getById("id_3")).thenThrow(new NotFoundException("Not found"));
    assertThrows(NotFoundException.class, () -> itemReferenceService.getStatisticalCodeById("id_3"));

    when(statisticalCodeClient.getByQuery("code==\"code_4\"")).thenReturn(new StatisticalCodeCollection().withStatisticalCodes(Collections.emptyList()));
    actual = itemReferenceService.getStatisticalCodeIdByCode("code_4");
    assertEquals("code_4", actual);

    assertEquals(EMPTY, itemReferenceService.getStatisticalCodeById(null));
    assertNull(itemReferenceService.getDamagedStatusIdByName(EMPTY));
  }

  @Test
  void testGetUserName() {
    when(userClient.getUserById("id_1")).thenReturn(new User().withUsername("name_1"));
    var actual = itemReferenceService.getUserNameById("id_1");
    assertEquals("name_1", actual);

    when(userClient.getUserByQuery("username==\"name_2\"", 1L)).thenReturn(new UserCollection().withUsers(Collections.singletonList(new User().withId("id_2"))));
    actual = itemReferenceService.getUserIdByUserName("name_2");
    assertEquals("id_2", actual);

    when(userClient.getUserById("id_3")).thenThrow(new NotFoundException("Not found"));
    assertThrows(NotFoundException.class, () -> itemReferenceService.getUserNameById("id_3"));

    when(userClient.getUserByQuery("username==\"name_4\"", 1L)).thenReturn(new UserCollection().withUsers(Collections.emptyList()));
    actual = itemReferenceService.getUserIdByUserName("name_4");
    assertEquals("name_4", actual);

    assertEquals(EMPTY, itemReferenceService.getUserNameById(null));
    assertNull(itemReferenceService.getUserIdByUserName(EMPTY));
  }

  @Test
  void testGetItemLocation() {
    when(locationClient.getLocationByQuery("name==\"name_1\"")).thenReturn(new ItemLocationCollection().withLocations(Collections.singletonList(new ItemLocation().withId("id_1"))));
    var actual = itemReferenceService.getLocationByName("name_1");
    assertEquals("id_1", actual.getId());

    when(locationClient.getLocationByQuery("name==\"name_2\"")).thenThrow(new NotFoundException("Not found"));
    assertThrows(NotFoundException.class, () -> itemReferenceService.getLocationByName("name_2"));
  }

  @Test
  void testGetMaterialType() {
    when(materialTypeClient.getByQuery("name==\"name_1\"")).thenReturn(new MaterialTypeCollection().withMtypes(Collections.singletonList(new MaterialType().withId("id_1"))));
    var actual = itemReferenceService.getMaterialTypeByName("name_1");
    assertEquals("id_1", actual.getId());

    when(materialTypeClient.getByQuery("name==\"name_2\"")).thenThrow(new NotFoundException("Not found"));
    assertThrows(NotFoundException.class, () -> itemReferenceService.getMaterialTypeByName("name_2"));
  }

  @Test
  void testGetLoanType() {
    when(loanTypeClient.getByQuery("name==\"name_1\"")).thenReturn(new LoanTypeCollection().withLoantypes(Collections.singletonList(new LoanType().withId("id_1"))));
    var actual = itemReferenceService.getLoanTypeByName("name_1");
    assertEquals("id_1", actual.getId());

    when(loanTypeClient.getByQuery("name==\"name_2\"")).thenThrow(new NotFoundException("Not found"));
    assertThrows(NotFoundException.class, () -> itemReferenceService.getLoanTypeByName("name_2"));
  }
}
