package org.folio.bulkops.service;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.Collections;

import org.folio.bulkops.BaseTest;
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
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;

@ExtendWith(MockitoExtension.class)
class ItemReferenceHelperTest extends BaseTest {

  @Autowired
  private ItemReferenceHelper itemReferenceHelper;

  @Test
  void testGetCallNumberTypeNameById() {
    when(callNumberTypeClient.getById("id_1")).thenReturn(new CallNumberType().withName("name_1"));
    var actual = itemReferenceHelper.getCallNumberTypeNameById("id_1");
    assertEquals("name_1", actual);

    when(callNumberTypeClient.getById("id_2")).thenThrow(new NotFoundException("Not found"));
    assertThrows(NotFoundException.class, () -> itemReferenceHelper.getCallNumberTypeNameById("id_2"));

    assertEquals(EMPTY, itemReferenceHelper.getCallNumberTypeNameById(null));
  }

  @Test
  void testGetDamagedStatus() {
    when(damagedStatusClient.getById("id_1")).thenReturn(new DamagedStatus().withName("name_1"));
    var actual = itemReferenceHelper.getDamagedStatusNameById("id_1");
    assertEquals("name_1", actual);

    when(damagedStatusClient.getByQuery("name==\"name_2\"")).thenReturn(new DamagedStatusCollection().withItemDamageStatuses(Collections.singletonList(new DamagedStatus().withId("id_2"))));
    actual = itemReferenceHelper.getDamagedStatusIdByName("name_2");
    assertEquals("id_2", actual);

    when(damagedStatusClient.getById("id_3")).thenThrow(new NotFoundException("Not found"));
    assertThrows(NotFoundException.class, () -> itemReferenceHelper.getDamagedStatusNameById("id_3"));

    when(damagedStatusClient.getByQuery("name==\"name_4\"")).thenReturn(new DamagedStatusCollection().withItemDamageStatuses(Collections.emptyList()));
    actual = itemReferenceHelper.getDamagedStatusIdByName("name_4");
    assertEquals("name_4", actual);

    assertEquals(EMPTY, itemReferenceHelper.getDamagedStatusNameById(null));
    assertNull(itemReferenceHelper.getDamagedStatusIdByName(EMPTY));
  }

  @Test
  void testGetNoteType() {
    when(itemNoteTypeClient.getById("id_1")).thenReturn(new NoteType().withName("name_1"));
    var actual = itemReferenceHelper.getNoteTypeNameById("id_1");
    assertEquals("name_1", actual);

    when(itemNoteTypeClient.getByQuery("name==\"name_2\"")).thenReturn(new NoteTypeCollection().withItemNoteTypes(Collections.singletonList(new NoteType().withId("id_2"))));
    actual = itemReferenceHelper.getNoteTypeIdByName("name_2");
    assertEquals("id_2", actual);

    when(itemNoteTypeClient.getById("id_3")).thenThrow(new NotFoundException("Not found"));
    assertThrows(NotFoundException.class, () -> itemReferenceHelper.getNoteTypeNameById("id_3"));

    when(itemNoteTypeClient.getByQuery("name==\"name_4\"")).thenReturn(new NoteTypeCollection().withItemNoteTypes(Collections.emptyList()));
    actual = itemReferenceHelper.getNoteTypeIdByName("name_4");
    assertEquals("name_4", actual);

    assertEquals(EMPTY, itemReferenceHelper.getNoteTypeNameById(null));
    assertNull(itemReferenceHelper.getNoteTypeIdByName(EMPTY));
  }

  @Test
  void testGetServicePoint() {
    when(servicePointClient.getById("id_1")).thenReturn(new ServicePoint().withName("name_1"));
    var actual = itemReferenceHelper.getServicePointNameById("id_1");
    assertEquals("name_1", actual);

    when(servicePointClient.get("name==\"name_2\"", 1L)).thenReturn(new ServicePoints().withServicepoints(Collections.singletonList(new ServicePoint().withId("id_2"))));
    actual = itemReferenceHelper.getServicePointIdByName("name_2");
    assertEquals("id_2", actual);

    when(servicePointClient.getById("id_3")).thenThrow(new NotFoundException("Not found"));
    assertThrows(NotFoundException.class, () -> itemReferenceHelper.getServicePointNameById("id_3"));

    when(servicePointClient.get("name==\"name_4\"", 1L)).thenReturn(new ServicePoints().withServicepoints(Collections.emptyList()));
    actual = itemReferenceHelper.getServicePointIdByName("name_4");
    assertEquals("name_4", actual);

    assertEquals(EMPTY, itemReferenceHelper.getServicePointNameById(null));
    assertNull(itemReferenceHelper.getServicePointIdByName(EMPTY));
  }

  @Test
  void testGetStatisticalCode() {
    when(statisticalCodeClient.getById("id_1")).thenReturn(new StatisticalCode().withCode("code_1"));
    var actual = itemReferenceHelper.getStatisticalCodeById("id_1");
    assertEquals("code_1", actual);

    when(statisticalCodeClient.getByQuery("code==\"code_2\"")).thenReturn(new StatisticalCodeCollection().withStatisticalCodes(Collections.singletonList(new StatisticalCode().withId("id_2"))));
    actual = itemReferenceHelper.getStatisticalCodeIdByCode("code_2");
    assertEquals("id_2", actual);

    when(statisticalCodeClient.getById("id_3")).thenThrow(new NotFoundException("Not found"));
    assertThrows(NotFoundException.class, () -> itemReferenceHelper.getStatisticalCodeById("id_3"));

    when(statisticalCodeClient.getByQuery("code==\"code_4\"")).thenReturn(new StatisticalCodeCollection().withStatisticalCodes(Collections.emptyList()));
    actual = itemReferenceHelper.getStatisticalCodeIdByCode("code_4");
    assertEquals("code_4", actual);

    assertEquals(EMPTY, itemReferenceHelper.getStatisticalCodeById(null));
    assertNull(itemReferenceHelper.getDamagedStatusIdByName(EMPTY));
  }

  @Test
  void testGetUserName() {
    when(userClient.getUserById("id_1")).thenReturn(new User().withUsername("name_1"));
    var actual = itemReferenceHelper.getUserNameById("id_1");
    assertEquals("name_1", actual);

    when(userClient.getUserByQuery("username==\"name_2\"", 1L)).thenReturn(new UserCollection().withUsers(Collections.singletonList(new User().withId("id_2"))));
    actual = itemReferenceHelper.getUserIdByUserName("name_2");
    assertEquals("id_2", actual);

    when(userClient.getUserById("id_3")).thenThrow(new NotFoundException("Not found"));
    assertThrows(NotFoundException.class, () -> itemReferenceHelper.getUserNameById("id_3"));

    when(userClient.getUserByQuery("username==\"name_4\"", 1L)).thenReturn(new UserCollection().withUsers(Collections.emptyList()));
    actual = itemReferenceHelper.getUserIdByUserName("name_4");
    assertEquals("name_4", actual);

    assertEquals(EMPTY, itemReferenceHelper.getUserNameById(null));
    assertNull(itemReferenceHelper.getUserIdByUserName(EMPTY));
  }

  @Test
  void testGetItemLocation() {
    when(locationClient.getLocationByQuery("name==\"name_1\"")).thenReturn(new ItemLocationCollection().withLocations(Collections.singletonList(new ItemLocation().withId("id_1"))));
    var actual = itemReferenceHelper.getLocationByName("name_1");
    assertEquals("id_1", actual.getId());

    when(locationClient.getLocationByQuery("name==\"name_2\"")).thenThrow(new NotFoundException("Not found"));
    assertThrows(NotFoundException.class, () -> itemReferenceHelper.getLocationByName("name_2"));
  }

  @Test
  void testGetMaterialType() {
    when(materialTypeClient.getByQuery("name==\"name_1\"")).thenReturn(new MaterialTypeCollection().withMtypes(Collections.singletonList(new MaterialType().withId("id_1"))));
    var actual = itemReferenceHelper.getMaterialTypeByName("name_1");
    assertEquals("id_1", actual.getId());

    when(materialTypeClient.getByQuery("name==\"name_2\"")).thenThrow(new NotFoundException("Not found"));
    assertThrows(NotFoundException.class, () -> itemReferenceHelper.getMaterialTypeByName("name_2"));
  }

  @Test
  void testGetLoanType() {
    when(loanTypeClient.getByQuery("name==\"name_1\"")).thenReturn(new LoanTypeCollection().withLoantypes(Collections.singletonList(new LoanType().withId("id_1"))));
    var actual = itemReferenceHelper.getLoanTypeByName("name_1");
    assertEquals("id_1", actual.getId());

    when(loanTypeClient.getByQuery("name==\"name_2\"")).thenThrow(new NotFoundException("Not found"));
    assertThrows(NotFoundException.class, () -> itemReferenceHelper.getLoanTypeByName("name_2"));
  }
}
