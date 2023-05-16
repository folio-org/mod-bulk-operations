package org.folio.bulkops.service;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.folio.bulkops.util.Utils.encode;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
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
    var actual = userReferenceHelper.getAddressTypeDescById("id_1");
    assertEquals("desc", actual);

    when(addressTypeClient.getAddressTypeById("id_2")).thenThrow(new NotFoundException("not found"));
    actual = userReferenceHelper.getAddressTypeDescById("id_2");
    assertEquals("id_2", actual);

    when(addressTypeClient.getByQuery(encode("desc==\"desc_1\""))).thenReturn(AddressTypeCollection.builder().addressTypes(singletonList(AddressType.builder().id("id_3").build())).build());
    actual = userReferenceHelper.getAddressTypeIdByDesc("desc_1");
    assertEquals("id_3", actual);

    when(addressTypeClient.getByQuery(encode("desc==\"desc_2\""))).thenReturn(AddressTypeCollection.builder().addressTypes(emptyList()).build());
    actual = userReferenceHelper.getAddressTypeIdByDesc("desc_2");
    assertEquals("desc_2", actual);

    assertEquals(EMPTY, userReferenceHelper.getAddressTypeDescById(null));
    assertNull(userReferenceHelper.getAddressTypeIdByDesc(EMPTY));
  }

  @Test
  void testGetDepartment() {
    when (departmentClient.getDepartmentById("id_1")).thenReturn(Department.builder().name("name_1").build());
    var actual = userReferenceHelper.getDepartmentNameById("id_1");
    assertEquals("name_1", actual);

    when(departmentClient.getDepartmentById("id_2")).thenThrow(new NotFoundException("not found"));
    actual = userReferenceHelper.getDepartmentNameById("id_2");
    assertEquals("id_2", actual);

    when(departmentClient.getByQuery(encode("name==\"name_2\""))).thenReturn(DepartmentCollection.builder().departments(singletonList(Department.builder().id("id_3").build())).build());
    actual = userReferenceHelper.getDepartmentIdByName("name_2");
    assertEquals("id_3", actual);

    when(departmentClient.getByQuery(encode("name==\"name_3\""))).thenReturn(DepartmentCollection.builder().departments(emptyList()).build());
    actual = userReferenceHelper.getDepartmentIdByName("name_3");
    assertEquals("name_3", actual);

    assertEquals(EMPTY, userReferenceHelper.getDepartmentNameById(null));
    assertNull(userReferenceHelper.getDepartmentIdByName(EMPTY));
  }

  @Test
  void testGetPatronGroup() {
    when (groupClient.getGroupById("id_1")).thenReturn(UserGroup.builder().group("name_1").build());
    var actual = userReferenceHelper.getPatronGroupNameById("id_1");
    assertEquals("name_1", actual);

    when(groupClient.getGroupById("id_2")).thenThrow(new NotFoundException("not found"));
    actual = userReferenceHelper.getPatronGroupNameById("id_2");
    assertEquals("id_2", actual);

    when(groupClient.getByQuery(encode("group==\"name_2\""))).thenReturn(UserGroupCollection.builder().usergroups(singletonList(UserGroup.builder().id("id_3").build())).build());
    actual = userReferenceHelper.getPatronGroupIdByName("name_2");
    assertEquals("id_3", actual);

    when(groupClient.getByQuery(encode("group==\"name_3\""))).thenReturn(UserGroupCollection.builder().usergroups(emptyList()).build());
    actual = userReferenceHelper.getPatronGroupIdByName("name_3");
    assertEquals("name_3", actual);

    assertEquals(EMPTY, userReferenceHelper.getPatronGroupNameById(null));
    assertNull(userReferenceHelper.getPatronGroupIdByName(EMPTY));
  }
}
