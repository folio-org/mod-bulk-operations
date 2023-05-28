package org.folio.bulkops.service;

import static org.folio.bulkops.util.Utils.encode;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
    var actual = userReferenceService.getAddressTypeById("id");
    verify(addressTypeClient).getAddressTypeById("id");
    assertEquals("type", actual.getDesc());

    when(addressTypeClient.getAddressTypeById("id")).thenThrow(NotFoundException.class);
    assertThrows(NotFoundException.class, () -> userReferenceService.getAddressTypeById("id"));
  }

  @Test
  void getAddressTypeIdByDescTest() {
    var expected = UUID.randomUUID().toString();
    when(addressTypeClient.getByQuery("desc==" + encode("*"))).thenReturn(new AddressTypeCollection().withAddressTypes(List.of(new AddressType().withId(expected))));
    var actual = userReferenceService.getAddressTypeByDesc("*");
    verify(addressTypeClient).getByQuery("desc==" + encode("*"));
    assertEquals(expected, actual.getId());
  }

  @Test
  void getAddressTypeDescByIdIfNullTest() {
    userReferenceService.getAddressTypeById(null);
    verify(addressTypeClient, times(0)).getAddressTypeById(isA(String.class));
  }

  @Test
  void getDepartmentNameByIdTest() {
    when(departmentClient.getDepartmentById("id")).thenReturn(new Department().withName("departmentName"));
    var actual = userReferenceService.getDepartmentById("id");
    verify(departmentClient).getDepartmentById("id");
    assertEquals("departmentName", actual.getName());

    when(departmentClient.getDepartmentById("id")).thenThrow(NotFoundException.class);
    assertThrows(NotFoundException.class, () -> userReferenceService.getDepartmentById("id"));
  }

  @Test
  void getDepartmentIdByNameTest() {
    var expected = UUID.randomUUID().toString();
    when(departmentClient.getByQuery("name==" + encode("*"))).thenReturn(new DepartmentCollection().withDepartments(List.of(new Department().withId(expected))));
    var actual = userReferenceService.getDepartmentByName("*");
    verify(departmentClient).getByQuery("name==" + encode("*"));
    assertEquals(expected, actual.getId());
  }

  @Test
  void getDepartmentNameByIdIfNullTest() {
    userReferenceService.getDepartmentById(null);

    verify(departmentClient, times(0)).getDepartmentById(isA(String.class));
  }

  @Test
  void getPatronGroupNameByIdTest() {
    when(groupClient.getGroupById("id")).thenReturn(new UserGroup().withGroup("userGroup"));
    var actual = userReferenceService.getPatronGroupById("id");
    verify(groupClient).getGroupById("id");
    assertEquals("userGroup", actual.getGroup());

    when(groupClient.getGroupById("id")).thenThrow(NotFoundException.class);
    assertThrows(NotFoundException.class, () -> userReferenceService.getPatronGroupById("id"));
  }

  @Test
  void getPatronGroupIdByNameTest() {
    var expected = UUID.randomUUID().toString();
    when(groupClient.getByQuery("group==" + encode("*"))).thenReturn(new UserGroupCollection().withUsergroups(List.of(new UserGroup().withId(expected))));
    var actual = userReferenceService.getPatronGroupByName("*");
    verify(groupClient).getByQuery("group==" + encode("*"));
    assertEquals(expected, actual.getId());
  }

  @Test
  void getCustomFieldByRefIdTest() {
    var customField = new CustomField().withRefId("refId").withName("name");
    when(customFieldsClient.getByQuery(isA(String.class), eq("refId==" + encode("refId"))))
      .thenReturn(new CustomFieldCollection().withCustomFields(List.of(customField)));
    doReturn("module").when(userReferenceService).getModuleId(isA(String.class));

    var actual = userReferenceService.getCustomFieldByRefId("refId");
    assertEquals(customField, actual);
  }

  @Test
  void getCustomFieldByNameTest() {
    var customField = new CustomField().withRefId("refId").withName("name");
    when(customFieldsClient.getByQuery(isA(String.class), eq("name==" + encode("name"))))
      .thenReturn(new CustomFieldCollection().withCustomFields(List.of(customField)));
    doReturn("module").when(userReferenceService).getModuleId(isA(String.class));

    var actual = userReferenceService.getCustomFieldByName("name");
    assertEquals(customField, actual);

    when(customFieldsClient.getByQuery(isA(String.class), eq("name==" + encode("name"))))
      .thenReturn(new CustomFieldCollection().withCustomFields(new ArrayList<>()));
    doReturn("module").when(userReferenceService).getModuleId(isA(String.class));
    assertThrows(NotFoundException.class, () -> userReferenceService.getCustomFieldByName("name"));
  }
}
