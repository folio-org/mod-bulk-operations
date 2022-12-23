package org.folio.bulkops.adapters.impl.users;

import org.folio.bulkops.client.AddressTypeClient;
import org.folio.bulkops.client.CustomFieldsClient;
import org.folio.bulkops.client.DepartmentClient;
import org.folio.bulkops.client.GroupClient;
import org.folio.bulkops.domain.dto.AddressType;
import org.folio.bulkops.domain.dto.CustomField;
import org.folio.bulkops.domain.dto.CustomFieldCollection;
import org.folio.bulkops.domain.dto.Department;
import org.folio.bulkops.domain.dto.UserGroup;
import org.folio.bulkops.error.BulkOperationException;
import org.folio.bulkops.error.NotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserReferenceResolverTest {

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
  private UserReferenceResolver userReferenceResolver;

  @Test
  void getAddressTypeDescByIdTest() {
    when(addressTypeClient.getAddressTypeById("id")).thenReturn(new AddressType().desc("type"));
    var actual = userReferenceResolver.getAddressTypeDescById("id", null, null);
    verify(addressTypeClient).getAddressTypeById("id");
    assertEquals("type", actual);

    when(addressTypeClient.getAddressTypeById("id")).thenThrow(NotFoundException.class);
    actual = userReferenceResolver.getAddressTypeDescById("id", null, null);
    assertEquals("id", actual);
  }

  @Test
  void getAddressTypeDescByIdIfNullTest() {
    userReferenceResolver.getAddressTypeDescById(null, null, null);
    verify(addressTypeClient, times(0)).getAddressTypeById(isA(String.class));
  }

  @Test
  void getDepartmentNameByIdTest() {
    when(departmentClient.getDepartmentById("id")).thenReturn(new Department().name("departmentName"));
    var actual = userReferenceResolver.getDepartmentNameById("id", null, null);
    verify(departmentClient).getDepartmentById("id");
    assertEquals("departmentName", actual);

    when(departmentClient.getDepartmentById("id")).thenThrow(NotFoundException.class);
    actual = userReferenceResolver.getDepartmentNameById("id", null, null);
    assertEquals("id", actual);
  }

  @Test
  void getDepartmentNameByIdIfNullTest() {
    userReferenceResolver.getDepartmentNameById(null, null, null);

    verify(departmentClient, times(0)).getDepartmentById(isA(String.class));
  }

  @Test
  void getPatronGroupNameByIdTest() {
    when(groupClient.getGroupById("id")).thenReturn(new UserGroup().group("userGroup"));
    var actual = userReferenceResolver.getPatronGroupNameById("id", null, null);
    verify(groupClient).getGroupById("id");
    assertEquals("userGroup", actual);

    when(groupClient.getGroupById("id")).thenThrow(NotFoundException.class);
    actual = userReferenceResolver.getPatronGroupNameById("id", null, null);
    assertEquals("id", actual);
  }

  @Test
  void getPatronGroupNameByIdIfNullTest() {
    userReferenceResolver.getPatronGroupNameById(null, null, null);

    verify(groupClient, times(0)).getGroupById(isA(String.class));
  }

  @Test
  void getCustomFieldByRefIdTest() {
    var customField = new CustomField().refId("refId").name("name");
    when(customFieldsClient.getCustomFieldsByQuery(isA(String.class), eq("refId==\"refId\"")))
      .thenReturn(new CustomFieldCollection().customFields(List.of(customField)));
    when(customFieldsClient.getCustomFieldsByQuery(isA(String.class), eq("refId==\"refId2\"")))
      .thenReturn(new CustomFieldCollection().customFields(List.of()));
    doReturn("module").when(userReferenceResolver).getModuleId(isA(String.class));

    var actual = userReferenceResolver.getCustomFieldByRefId("refId");
    assertEquals(customField, actual);

    assertThrows(BulkOperationException.class, () -> {
      userReferenceResolver.getCustomFieldByRefId("refId2");
    });
  }
}
