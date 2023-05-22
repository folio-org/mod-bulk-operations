package org.folio.bulkops.service;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.folio.bulkops.util.Utils.encode;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.folio.bulkops.client.AddressTypeClient;
import org.folio.bulkops.client.CustomFieldsClient;
import org.folio.bulkops.client.DepartmentClient;
import org.folio.bulkops.client.GroupClient;
import org.folio.bulkops.domain.bean.AddressType;
import org.folio.bulkops.domain.bean.AddressTypeCollection;
import org.folio.bulkops.domain.bean.CustomField;
import org.folio.bulkops.domain.bean.CustomFieldCollection;
import org.folio.bulkops.domain.bean.Department;
import org.folio.bulkops.domain.bean.DepartmentCollection;
import org.folio.bulkops.domain.bean.UserGroup;
import org.folio.bulkops.domain.bean.UserGroupCollection;
import org.folio.bulkops.exception.NotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserReferenceServiceTest {

  @Mock
  private AddressTypeClient addressTypeClient;
  @Mock
  private DepartmentClient departmentClient;
  @Mock
  private GroupClient groupClient;
  @Mock
  private CustomFieldsClient customFieldsClient;
  @InjectMocks
  @Spy
  private UserReferenceService userReferenceService;

  @Test
  void getAddressTypeDescByIdTest() {
    when(addressTypeClient.getAddressTypeById("id")).thenReturn(new AddressType().withDesc("type"));
    var actual = userReferenceService.getAddressTypeDescById("id");
    verify(addressTypeClient).getAddressTypeById("id");
    assertEquals("type", actual);

    when(addressTypeClient.getAddressTypeById("id")).thenThrow(NotFoundException.class);
    assertThrows(NotFoundException.class, () -> userReferenceService.getAddressTypeDescById("id"));
  }

  @Test
  void getAddressTypeIdByDescTest() {
    var expected = UUID.randomUUID().toString();
    when(addressTypeClient.getByQuery("desc==\"*\"")).thenReturn(new AddressTypeCollection().withAddressTypes(List.of(new AddressType().withId(expected))));
    var actual = userReferenceService.getAddressTypeIdByDesc("*");
    verify(addressTypeClient).getByQuery("desc==\"*\"");
    assertEquals(expected, actual);

    when(addressTypeClient.getByQuery("desc==\"*\"")).thenReturn(new AddressTypeCollection());
    assertEquals(EMPTY, userReferenceService.getAddressTypeIdByDesc("*"));
  }

  @Test
  void getAddressTypeDescByIdIfNullTest() {
    userReferenceService.getAddressTypeDescById(null);
    verify(addressTypeClient, times(0)).getAddressTypeById(isA(String.class));
  }

  @Test
  void getDepartmentNameByIdTest() {
    when(departmentClient.getDepartmentById("id")).thenReturn(new Department().withName("departmentName"));
    var actual = userReferenceService.getDepartmentNameById("id");
    verify(departmentClient).getDepartmentById("id");
    assertEquals("departmentName", actual);

    when(departmentClient.getDepartmentById("id")).thenThrow(NotFoundException.class);
    assertThrows(NotFoundException.class, () -> userReferenceService.getDepartmentNameById("id"));
  }

  @Test
  void getDepartmentIdByNameTest() {
    var expected = UUID.randomUUID().toString();
    when(departmentClient.getByQuery("name==\"*\"")).thenReturn(new DepartmentCollection().withDepartments(List.of(new Department().withId(expected))));
    var actual = userReferenceService.getDepartmentIdByName("*");
    verify(departmentClient).getByQuery("name==\"*\"");
    assertEquals(expected, actual);

    when(departmentClient.getByQuery("name==\"*\"")).thenReturn(new DepartmentCollection());
    assertEquals(EMPTY, userReferenceService.getDepartmentIdByName("*"));
  }

  @Test
  void getDepartmentNameByIdIfNullTest() {
    userReferenceService.getDepartmentNameById(null);

    verify(departmentClient, times(0)).getDepartmentById(isA(String.class));
  }

  @Test
  void getPatronGroupNameByIdTest() {
    when(groupClient.getGroupById("id")).thenReturn(new UserGroup().withGroup("userGroup"));
    var actual = userReferenceService.getPatronGroupNameById("id");
    verify(groupClient).getGroupById("id");
    assertEquals("userGroup", actual);

    when(groupClient.getGroupById("id")).thenThrow(NotFoundException.class);
    assertThrows(NotFoundException.class, () -> userReferenceService.getPatronGroupNameById("id"));
  }

  @Test
  void getPatronGroupIdByNameTest() {
    var expected = UUID.randomUUID().toString();
    when(groupClient.getByQuery("group==\"*\"")).thenReturn(new UserGroupCollection().withUsergroups(List.of(new UserGroup().withId(expected))));
    var actual = userReferenceService.getPatronGroupIdByName("*");
    verify(groupClient).getByQuery("group==\"*\"");
    assertEquals(expected, actual);

    when(groupClient.getByQuery("group==\"*\"")).thenReturn(new UserGroupCollection().withUsergroups(new ArrayList<>()));
    assertEquals(EMPTY, userReferenceService.getPatronGroupIdByName("*"));
  }

  @Test
  void getPatronGroupNameByIdIfNullTest() {
    userReferenceService.getPatronGroupNameById(null);

    verify(groupClient, times(0)).getGroupById(isA(String.class));
  }

  @Test
  void getCustomFieldByRefIdTest() {
    var customField = new CustomField().withRefId("refId").withName("name");
    when(customFieldsClient.getByQuery(isA(String.class), eq(encode("refId==\"refId\""))))
      .thenReturn(new CustomFieldCollection().withCustomFields(List.of(customField)));
    when(customFieldsClient.getByQuery(isA(String.class), eq(encode("refId==\"refId2\""))))
      .thenReturn(new CustomFieldCollection().withCustomFields(List.of()));
    doReturn("module").when(userReferenceService).getModuleId(isA(String.class));

    var actual = userReferenceService.getCustomFieldByRefId("refId");
    assertEquals(customField, actual);

    assertEquals(new CustomField(), userReferenceService.getCustomFieldByRefId("refId2"));
  }

  @Test
  void getCustomFieldByNameTest() {
    var customField = new CustomField().withRefId("refId").withName("name");
    when(customFieldsClient.getByQuery(isA(String.class), eq(encode("name==\"name\""))))
      .thenReturn(new CustomFieldCollection().withCustomFields(List.of(customField)));
    doReturn("module").when(userReferenceService).getModuleId(isA(String.class));

    var actual = userReferenceService.getCustomFieldByName("name");
    assertEquals(customField, actual);

    when(customFieldsClient.getByQuery(isA(String.class), eq(encode("name==\"name\""))))
      .thenReturn(new CustomFieldCollection().withCustomFields(new ArrayList<>()));
    doReturn("module").when(userReferenceService).getModuleId(isA(String.class));
    assertNull(userReferenceService.getCustomFieldByName("name"));
  }
}
