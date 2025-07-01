package org.folio.bulkops.service;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.folio.bulkops.util.Utils.encode;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import lombok.SneakyThrows;
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
import org.folio.bulkops.exception.NotFoundException;
import org.folio.bulkops.exception.ReferenceDataNotFoundException;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.integration.XOkapiHeaders;
import org.folio.spring.scope.FolioExecutionContextSetter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

@ExtendWith(MockitoExtension.class)
class ItemReferenceHelperTest extends BaseTest {

  @Autowired
  private ItemReferenceHelper itemReferenceHelper;
  @MockitoSpyBean
  private FolioExecutionContext folioExecutionContext;

  @Test
  void testGetCallNumberTypeNameById() {
    when(callNumberTypeClient.getById("id_1")).thenReturn(new CallNumberType().withName("name_1"));
    var actual = itemReferenceHelper.getCallNumberTypeNameById("id_1");
    assertEquals("name_1", actual);

    when(callNumberTypeClient.getById("id_2")).thenThrow(new NotFoundException("Not found"));
    assertThrows(ReferenceDataNotFoundException.class, () -> itemReferenceHelper.getCallNumberTypeNameById("id_2"));

    assertEquals(EMPTY, itemReferenceHelper.getCallNumberTypeNameById(null));
  }

  @Test
  void testGetDamagedStatus() {
    when(damagedStatusClient.getById("id_1")).thenReturn(new DamagedStatus().withName("name_1"));
    var actual = itemReferenceHelper.getDamagedStatusById("id_1");
    assertEquals("name_1", actual.getName());

    when(damagedStatusClient.getByQuery("name==" + encode("name_2"))).thenReturn(new DamagedStatusCollection().withItemDamageStatuses(Collections.singletonList(new DamagedStatus().withId("id_2"))));
    actual = itemReferenceHelper.getDamagedStatusByName("name_2");
    assertEquals("id_2", actual.getId());

    when(damagedStatusClient.getById("id_3")).thenThrow(new NotFoundException("Not found"));
    assertThrows(ReferenceDataNotFoundException.class, () -> itemReferenceHelper.getDamagedStatusById("id_3"));

    when(damagedStatusClient.getByQuery("name==" + encode("name_4"))).thenReturn(new DamagedStatusCollection().withItemDamageStatuses(Collections.singletonList(new DamagedStatus().withName("name_4"))));
    actual = itemReferenceHelper.getDamagedStatusByName("name_4");
    assertEquals("name_4", actual.getName());
  }

  @Test
  void testGetNoteType() {
    try (var context =  new FolioExecutionContextSetter(folioExecutionContext)) {
      when(itemNoteTypeClient.getNoteTypeById("id_1")).thenReturn(new NoteType().withName("name_1"));
      var actual = itemReferenceHelper.getNoteTypeNameById("id_1", "tenant");
      assertEquals("name_1", actual);

      when(itemNoteTypeClient.getNoteTypesByQuery("name==\"name_2\"", 1)).thenReturn(new NoteTypeCollection().withItemNoteTypes(Collections.singletonList(new NoteType().withId("id_2"))));
      actual = itemReferenceHelper.getNoteTypeIdByName("name_2");
      assertEquals("id_2", actual);

      when(itemNoteTypeClient.getNoteTypeById("id_3")).thenThrow(new NotFoundException("Not found"));
      assertThrows(ReferenceDataNotFoundException.class, () -> itemReferenceHelper.getNoteTypeNameById("id_3", "tenant"));

      when(itemNoteTypeClient.getNoteTypesByQuery("name==\"name_4\"", 1)).thenReturn(new NoteTypeCollection().withItemNoteTypes(Collections.emptyList()));
      assertThrows(ReferenceDataNotFoundException.class, () -> itemReferenceHelper.getNoteTypeIdByName("name_4"));
    }
  }

  @Test
  void testGetServicePoint() {
    when(servicePointClient.getById("id_1")).thenReturn(new ServicePoint().withName("name_1"));
    var actual = itemReferenceHelper.getServicePointById("id_1");
    assertEquals("name_1", actual.getName());

    when(servicePointClient.getByQuery("name==\"name_2\"", 1L)).thenReturn(new ServicePoints().withServicepoints(Collections.singletonList(new ServicePoint().withId("id_2"))));
    actual = itemReferenceHelper.getServicePointByName("name_2");
    assertEquals("id_2", actual.getId());

    when(servicePointClient.getById("id_3")).thenThrow(new NotFoundException("Not found"));
    assertThrows(ReferenceDataNotFoundException.class, () -> itemReferenceHelper.getServicePointById("id_3"));

    when(servicePointClient.getByQuery("name==\"name_4\"", 1L)).thenReturn(new ServicePoints().withServicepoints(Collections.emptyList()));
    assertThrows(ReferenceDataNotFoundException.class, () -> itemReferenceHelper.getServicePointByName("name_4"));
  }

  @Test
  void testGetStatisticalCode() {
    HashMap<String, Collection<String>> headers = new HashMap<>();
    headers.put(XOkapiHeaders.TENANT, List.of("diku"));
    when(folioExecutionContext.getOkapiHeaders()).thenReturn(headers);
    when(folioExecutionContext.getTenantId()).thenReturn("diku");
    when(folioExecutionContext.getAllHeaders()).thenReturn(headers);
    when(statisticalCodeClient.getById("id_1")).thenReturn(new StatisticalCode().withCode("code_1"));
    var actual = itemReferenceHelper.getStatisticalCodeById("id_1");
    assertEquals("code_1", actual);

    when(statisticalCodeClient.getByQuery("code==\"code_2\"")).thenReturn(new StatisticalCodeCollection().withStatisticalCodes(Collections.singletonList(new StatisticalCode().withId("id_2"))));
    actual = itemReferenceHelper.getStatisticalCodeIdByCode("code_2");
    assertEquals("id_2", actual);

    when(statisticalCodeClient.getById("id_3")).thenThrow(new NotFoundException("Not found"));
    assertThrows(ReferenceDataNotFoundException.class, () -> itemReferenceHelper.getStatisticalCodeById("id_3"));

    when(statisticalCodeClient.getByQuery("code==\"code_4\"")).thenReturn(new StatisticalCodeCollection().withStatisticalCodes(Collections.emptyList()));
    assertThrows(ReferenceDataNotFoundException.class, () -> itemReferenceHelper.getStatisticalCodeIdByCode("code_4"));
  }

  @Test
  void testGetUserName() {
    when(userClient.getUserById("id_1")).thenReturn(new User().withUsername("name_1"));
    var actual = itemReferenceHelper.getUserNameById("id_1");
    assertEquals("name_1", actual);

    when(userClient.getByQuery("username==\"name_2\"", 1L)).thenReturn(new UserCollection().withUsers(Collections.singletonList(new User().withId("id_2"))));
    actual = itemReferenceHelper.getUserIdByUserName("name_2");
    assertEquals("id_2", actual);

    when(userClient.getUserById("id_3")).thenThrow(new NotFoundException("Not found"));
    assertThrows(ReferenceDataNotFoundException.class, () -> itemReferenceHelper.getUserNameById("id_3"));

    when(userClient.getByQuery("username==\"name_4\"", 1L)).thenReturn(new UserCollection().withUsers(Collections.emptyList()));
    assertThrows(ReferenceDataNotFoundException.class, () -> itemReferenceHelper.getUserIdByUserName("name_4"));
  }

  @Test
  void testGetItemLocation() {
    when(locationClient.getByQuery("name==\"name_1\"")).thenReturn(new ItemLocationCollection().withLocations(Collections.singletonList(new ItemLocation().withId("id_1"))));
    var actual = itemReferenceHelper.getLocationByName("name_1");
    assertEquals("id_1", actual.getId());

    when(locationClient.getByQuery("name==\"name_2\"")).thenThrow(new NotFoundException("Not found"));
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

  @Test
  @SneakyThrows
  void getLoanTypeByIdReturnsCorrectLoanType() {
    try (var context =  new FolioExecutionContextSetter(folioExecutionContext)) {
      when(loanTypeClient.getLoanTypeById("id_1")).thenReturn(new LoanType().withName("Loan Type 1"));
      var actual = itemReferenceHelper.getLoanTypeById("id_1", "tenant");
      assertEquals("Loan Type 1", actual.getName());
    }
  }

  @Test
  @SneakyThrows
  void getLoanTypeByIdThrowsExceptionForInvalidId() {
    try (var context =  new FolioExecutionContextSetter(folioExecutionContext)) {
      when(loanTypeClient.getLoanTypeById("invalid_id")).thenThrow(new NotFoundException("Not found"));
      assertThrows(ReferenceDataNotFoundException.class, () -> itemReferenceHelper.getLoanTypeById("invalid_id", "tenant"));
    }
  }

  @Test
  @SneakyThrows
  void getMaterialTypeByIdReturnsCorrectMaterialType() {
    try (var context =  new FolioExecutionContextSetter(folioExecutionContext)) {
      when(materialTypeClient.getById("id_1")).thenReturn(new MaterialType().withName("Material Type 1"));
      var actual = itemReferenceHelper.getMaterialTypeById("id_1", "tenant");
      assertEquals("Material Type 1", actual.getName());
    }
  }

  @Test
  @SneakyThrows
  void getMaterialTypeByIdThrowsExceptionForInvalidId() {
    try (var context =  new FolioExecutionContextSetter(folioExecutionContext)) {
      when(materialTypeClient.getById("invalid_id")).thenThrow(new NotFoundException("Not found"));
      assertThrows(ReferenceDataNotFoundException.class, () -> itemReferenceHelper.getMaterialTypeById("invalid_id", "tenant"));
    }
  }

  @Test
  @SneakyThrows
  void getLocationByIdReturnsCorrectLocation() {
    try (var context =  new FolioExecutionContextSetter(folioExecutionContext)) {
      when(locationClient.getLocationById("id_1")).thenReturn(new ItemLocation().withName("Location 1"));
      var actual = itemReferenceHelper.getLocationById("id_1", "tenant");
      assertEquals("Location 1", actual.getName());
    }
  }

  @Test
  @SneakyThrows
  void getLocationByIdThrowsExceptionForInvalidId() {
    try (var context =  new FolioExecutionContextSetter(folioExecutionContext)) {
      when(locationClient.getLocationById("invalid_id")).thenThrow(new NotFoundException("Not found"));
      assertThrows(ReferenceDataNotFoundException.class, () -> itemReferenceHelper.getLocationById("invalid_id", "tenant"));
    }
  }
}
