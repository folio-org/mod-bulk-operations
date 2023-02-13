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
    when(callNumberTypeClient.getById("id")).thenReturn(new CallNumberType().withName("name"));
    var actual = itemReferenceService.getCallNumberTypeNameById("id");
    assertEquals("name", actual);

    when(callNumberTypeClient.getById("id")).thenThrow(new NotFoundException("Not found"));
    assertThrows(NotFoundException.class, () -> itemReferenceService.getCallNumberTypeNameById("id"));

    assertEquals(EMPTY, itemReferenceService.getCallNumberTypeNameById(null));
  }

  @Test
  void testGetDamagedStatus() {
    when(damagedStatusClient.getById("id")).thenReturn(new DamagedStatus().withName("name"));
    var actual = itemReferenceService.getDamagedStatusNameById("id");
    assertEquals("name", actual);

    when(damagedStatusClient.getByQuery("name==\"name\"")).thenReturn(new DamagedStatusCollection().withItemDamageStatuses(Collections.singletonList(new DamagedStatus().withId("id"))));
    actual = itemReferenceService.getDamagedStatusIdByName("name");
    assertEquals("id", actual);

    when(damagedStatusClient.getById("id")).thenThrow(new NotFoundException("Not found"));
    assertThrows(NotFoundException.class, () -> itemReferenceService.getDamagedStatusNameById("id"));

    when(damagedStatusClient.getByQuery("name==\"name\"")).thenReturn(new DamagedStatusCollection().withItemDamageStatuses(Collections.emptyList()));
    actual = itemReferenceService.getDamagedStatusIdByName("name");
    assertEquals("name", actual);

    assertEquals(EMPTY, itemReferenceService.getDamagedStatusNameById(null));
    assertNull(itemReferenceService.getDamagedStatusIdByName(EMPTY));
  }

  @Test
  void testGetNoteType() {
    when(itemNoteTypeClient.getById("id")).thenReturn(new NoteType().withName("name"));
    var actual = itemReferenceService.getNoteTypeNameById("id");
    assertEquals("name", actual);

    when(itemNoteTypeClient.getByQuery("name==\"name\"")).thenReturn(new NoteTypeCollection().withItemNoteTypes(Collections.singletonList(new NoteType().withId("id"))));
    actual = itemReferenceService.getNoteTypeIdByName("name");
    assertEquals("id", actual);

    when(itemNoteTypeClient.getById("id")).thenThrow(new NotFoundException("Not found"));
    assertThrows(NotFoundException.class, () -> itemReferenceService.getNoteTypeNameById("id"));

    when(itemNoteTypeClient.getByQuery("name==\"name\"")).thenReturn(new NoteTypeCollection().withItemNoteTypes(Collections.emptyList()));
    actual = itemReferenceService.getNoteTypeIdByName("name");
    assertEquals("name", actual);

    assertEquals(EMPTY, itemReferenceService.getNoteTypeNameById(null));
    assertNull(itemReferenceService.getNoteTypeIdByName(EMPTY));
  }

  @Test
  void testGetServicePoint() {
    when(servicePointClient.getById("id")).thenReturn(new ServicePoint().withName("name"));
    var actual = itemReferenceService.getServicePointNameById("id");
    assertEquals("name", actual);

    when(servicePointClient.get("name==\"name\"", 1L)).thenReturn(new ServicePoints().withServicepoints(Collections.singletonList(new ServicePoint().withId("id"))));
    actual = itemReferenceService.getServicePointIdByName("name");
    assertEquals("id", actual);

    when(servicePointClient.getById("id")).thenThrow(new NotFoundException("Not found"));
    assertThrows(NotFoundException.class, () -> itemReferenceService.getServicePointNameById("id"));

    when(servicePointClient.get("name==\"name\"", 1L)).thenReturn(new ServicePoints().withServicepoints(Collections.emptyList()));
    actual = itemReferenceService.getServicePointIdByName("name");
    assertEquals("name", actual);

    assertEquals(EMPTY, itemReferenceService.getServicePointNameById(null));
    assertNull(itemReferenceService.getServicePointIdByName(EMPTY));
  }

  @Test
  void testGetStatisticalCode() {
    when(statisticalCodeClient.getById("id")).thenReturn(new StatisticalCode().withCode("code"));
    var actual = itemReferenceService.getStatisticalCodeById("id");
    assertEquals("code", actual);

    when(statisticalCodeClient.getByQuery("code==\"code\"")).thenReturn(new StatisticalCodeCollection().withStatisticalCodes(Collections.singletonList(new StatisticalCode().withId("id"))));
    actual = itemReferenceService.getStatisticalCodeIdByCode("code");
    assertEquals("id", actual);

    when(statisticalCodeClient.getById("id")).thenThrow(new NotFoundException("Not found"));
    assertThrows(NotFoundException.class, () -> itemReferenceService.getStatisticalCodeById("id"));

    when(statisticalCodeClient.getByQuery("code==\"code\"")).thenReturn(new StatisticalCodeCollection().withStatisticalCodes(Collections.emptyList()));
    actual = itemReferenceService.getStatisticalCodeIdByCode("code");
    assertEquals("code", actual);

    assertEquals(EMPTY, itemReferenceService.getStatisticalCodeById(null));
    assertNull(itemReferenceService.getDamagedStatusIdByName(EMPTY));
  }

  @Test
  void testGetUserName() {
    when(userClient.getUserById("id")).thenReturn(new User().withUsername("name"));
    var actual = itemReferenceService.getUserNameById("id");
    assertEquals("name", actual);

    when(userClient.getUserByQuery("username==\"name\"", 1L)).thenReturn(new UserCollection().withUsers(Collections.singletonList(new User().withId("id"))));
    actual = itemReferenceService.getUserIdByUserName("name");
    assertEquals("id", actual);

    when(userClient.getUserById("id")).thenThrow(new NotFoundException("Not found"));
    assertThrows(NotFoundException.class, () -> itemReferenceService.getUserNameById("id"));

    when(userClient.getUserByQuery("username==\"name\"", 1L)).thenReturn(new UserCollection().withUsers(Collections.emptyList()));
    actual = itemReferenceService.getUserIdByUserName("name");
    assertEquals("name", actual);

    assertEquals(EMPTY, itemReferenceService.getUserNameById(null));
    assertNull(itemReferenceService.getUserIdByUserName(EMPTY));
  }

  @Test
  void testGetItemLocation() {
    when(locationClient.getLocationByQuery("name==\"name\"")).thenReturn(new ItemLocationCollection().withLocations(Collections.singletonList(new ItemLocation().withId("id"))));
    var actual = itemReferenceService.getLocationByName("name");
    assertEquals("id", actual.getId());

    when(locationClient.getLocationByQuery("name==\"name\"")).thenThrow(new NotFoundException("Not found"));
    assertThrows(NotFoundException.class, () -> itemReferenceService.getLocationByName("name"));
  }

  @Test
  void testGetMaterialType() {
    when(materialTypeClient.getByQuery("name==\"name\"")).thenReturn(new MaterialTypeCollection().withMtypes(Collections.singletonList(new MaterialType().withId("id"))));
    var actual = itemReferenceService.getMaterialTypeByName("name");
    assertEquals("id", actual.getId());

    when(materialTypeClient.getByQuery("name==\"name\"")).thenThrow(new NotFoundException("Not found"));
    assertThrows(NotFoundException.class, () -> itemReferenceService.getMaterialTypeByName("name"));
  }

  @Test
  void testGetLoanType() {
    when(loanTypeClient.getByQuery("name==\"name\"")).thenReturn(new LoanTypeCollection().withLoantypes(Collections.singletonList(new LoanType().withId("id"))));
    var actual = itemReferenceService.getLoanTypeByName("name");
    assertEquals("id", actual.getId());

    when(loanTypeClient.getByQuery("name==\"name\"")).thenThrow(new NotFoundException("Not found"));
    assertThrows(NotFoundException.class, () -> itemReferenceService.getLoanTypeByName("name"));
  }
}
