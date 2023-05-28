package org.folio.bulkops.service;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import org.folio.bulkops.BaseTest;
import org.folio.bulkops.domain.bean.AddressType;
import org.folio.bulkops.domain.bean.AddressTypeCollection;
import org.folio.bulkops.domain.bean.Department;
import org.folio.bulkops.domain.bean.DepartmentCollection;
import org.folio.bulkops.domain.bean.UserGroup;
import org.folio.bulkops.domain.bean.UserGroupCollection;
import org.folio.bulkops.exception.NotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;

@ExtendWith(MockitoExtension.class)
class UserReferenceHelperTest extends BaseTest {

  @Autowired
  private UserReferenceHelper userReferenceHelper;

  @Test
  void testGetAddressType() {
    when (addressTypeClient.getAddressTypeById("id_1")).thenReturn(AddressType.builder().desc("desc").build());
    var actual = userReferenceHelper.getAddressTypeById("id_1");
    assertEquals("desc", actual.getDesc());

    when(addressTypeClient.getAddressTypeById("id_2")).thenThrow(new NotFoundException("not found"));
    assertThrows(NotFoundException.class, () -> userReferenceHelper.getAddressTypeById("id_2"));

    when(addressTypeClient.getByQuery("desc==\"desc_1\"")).thenReturn(AddressTypeCollection.builder().addressTypes(singletonList(AddressType.builder().id("id_3").build())).build());
    actual = userReferenceHelper.getAddressTypeByDesc("desc_1");
    assertEquals("id_3", actual.getId());

  }

  @Test
  void testGetDepartment() {
    when (departmentClient.getDepartmentById("id_1")).thenReturn(Department.builder().name("name_1").build());
    var actual = userReferenceHelper.getDepartmentById("id_1");
    assertEquals("name_1", actual.getName());

    when(departmentClient.getDepartmentById("id_2")).thenThrow(new NotFoundException("not found"));
    assertThrows(NotFoundException.class, () -> userReferenceHelper.getDepartmentById("id_2"));

    when(departmentClient.getByQuery("name==\"name_2\"")).thenReturn(DepartmentCollection.builder().departments(singletonList(Department.builder().id("id_3").build())).build());
    actual = userReferenceHelper.getDepartmentByName("name_2");
    assertEquals("id_3", actual.getId());

    when(departmentClient.getByQuery("name==\"name_3\"")).thenReturn(DepartmentCollection.builder().departments(emptyList()).build());
    assertThrows(NotFoundException.class, () -> userReferenceHelper.getDepartmentByName("name_3"));
  }

  @Test
  void testGetPatronGroup() {
    when (groupClient.getGroupById("id_1")).thenReturn(UserGroup.builder().group("name_1").build());
    var actual = userReferenceHelper.getPatronGroupById("id_1");
    assertEquals("name_1", actual.getGroup());

    when(groupClient.getGroupById("id_2")).thenThrow(new NotFoundException("not found"));
    assertThrows(NotFoundException.class, () -> userReferenceHelper.getPatronGroupById("id_2"));

    when(groupClient.getByQuery("group==\"name_2\"")).thenReturn(UserGroupCollection.builder().usergroups(singletonList(UserGroup.builder().id("id_3").build())).build());
    actual = userReferenceHelper.getPatronGroupByName("name_2");
    assertEquals("id_3", actual.getId());
  }
}
