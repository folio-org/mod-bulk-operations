package org.folio.bulkops.adapters.impl.users;

import static org.folio.bulkops.adapters.BulkEditAdapterHelper.getValueFromTable;
import static org.folio.bulkops.adapters.Constants.USER_ACTIVE;
import static org.folio.bulkops.adapters.Constants.USER_ADDRESSES;
import static org.folio.bulkops.adapters.Constants.USER_CUSTOM_FIELDS;
import static org.folio.bulkops.adapters.Constants.USER_DEPARTMENTS;
import static org.folio.bulkops.adapters.Constants.USER_PATRON_GROUP;
import static org.folio.bulkops.adapters.Constants.USER_TAGS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.folio.bulkops.client.UserClient;
import org.folio.bulkops.domain.bean.Address;
import org.folio.bulkops.domain.bean.CustomField;
import org.folio.bulkops.domain.bean.CustomFieldTypes;
import org.folio.bulkops.domain.bean.Personal;
import org.folio.bulkops.domain.bean.SelectField;
import org.folio.bulkops.domain.bean.SelectFieldOption;
import org.folio.bulkops.domain.bean.SelectFieldOptions;
import org.folio.bulkops.domain.bean.Tags;
import org.folio.bulkops.domain.bean.User;
import org.folio.bulkops.domain.bean.UserCollection;
import org.folio.bulkops.domain.bean.IdentifierType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserModClientAdapterTest {

  @Mock
  private UserReferenceResolver userReferenceResolver;
  @Mock
  private UserClient userClient;

  @InjectMocks
  private UserModClientAdapter userModClientAdapter;

  @Test
  void convertEntityToUnifiedTableTest() {
    var user = new User()
      .withActive(true)
      .withPersonal(new Personal().withAddresses(Collections.singletonList(new Address().withAddressTypeId("db541cda-fcc7-403b-8077-3613f3244901"))))
      .withPatronGroup("3684a786-6671-4268-8ed0-9db82ebca60b")
      .withDepartments(Set.of(UUID.fromString("103aee0f-c5f6-44de-94aa-74093f0e45d9")))
      .withTags(new Tags().withTagList(List.of("tag")))
      .withCustomFields(Map.of("refId1", true, "refId2", new ArrayList<>(List.of("one", "two")), "refId3", "short"));
    var customField1 = new CustomField();
    customField1.setRefId("refId1");
    customField1.setName("field1");
    customField1.setType(CustomFieldTypes.SINGLE_CHECKBOX);
    var customField2 = new CustomField();
    customField2.setRefId("refId2");
    customField2.setName("field2");
    customField2.setSelectField(new SelectField()
      .withOptions(new SelectFieldOptions().withValues(List.of(new SelectFieldOption().withId("one").withValue("one"), new SelectFieldOption().withId("two").withValue("two")))));
    customField2.setType(CustomFieldTypes.MULTI_SELECT_DROPDOWN);
    var customField3 = new CustomField();
    customField3.setRefId("refId3");
    customField3.setName("field3");
    customField3.setType(CustomFieldTypes.TEXTBOX_SHORT);

    when(userReferenceResolver.getDepartmentNameById(eq("103aee0f-c5f6-44de-94aa-74093f0e45d9"), isNull(), isNull())).thenReturn("departmentName");
    when(userReferenceResolver.getPatronGroupNameById(eq("3684a786-6671-4268-8ed0-9db82ebca60b"), isNull(), isNull())).thenReturn("patronGroupName");
    when(userReferenceResolver.getAddressTypeDescById(eq("db541cda-fcc7-403b-8077-3613f3244901"), isNull(), isNull())).thenReturn("addressType");

    when(userReferenceResolver.getCustomFieldByRefId("refId1")).thenReturn(customField1);
    when(userReferenceResolver.getCustomFieldByRefId("refId2")).thenReturn(customField2);
    when(userReferenceResolver.getCustomFieldByRefId("refId3")).thenReturn(customField3);

    var unifiedTable = userModClientAdapter.convertEntityToUnifiedTable(user, null, IdentifierType.ID);

    var header = unifiedTable.getHeader();
    var rows = unifiedTable.getRows();

    assertEquals(25, header.size());
    assertEquals(1, rows.size());
    assertEquals(25, rows.get(0). getRow().size());

    assertEquals("true", getValueFromTable(USER_ACTIVE, unifiedTable));
    assertEquals("patronGroupName", getValueFromTable(USER_PATRON_GROUP, unifiedTable));
    assertEquals("departmentName", getValueFromTable(USER_DEPARTMENTS, unifiedTable));
    assertEquals(";;;;;;;;addressType", getValueFromTable(USER_ADDRESSES, unifiedTable));
    assertEquals("tag", getValueFromTable(USER_TAGS, unifiedTable));
    assertTrue(getValueFromTable(USER_CUSTOM_FIELDS, unifiedTable).contains("field2:one;two"));
    assertTrue(getValueFromTable(USER_CUSTOM_FIELDS, unifiedTable).contains("field1:true"));
    assertTrue(getValueFromTable(USER_CUSTOM_FIELDS, unifiedTable).contains("field3:short"));
  }

  @Test
  void getUnifiedRepresentationByQueryTest() {
    var user1 = new User().withId("id").withBarcode("barcode").withPersonal(new Personal());
    var user2 = new User().withId("id2").withBarcode("barcode2").withPersonal(new Personal());
    var userCollection = new UserCollection();
    userCollection.setUsers(List.of(user1, user2));
    when(userClient.getUserByQuery("query", 1, 2)).thenReturn(userCollection);

    var unifiedTable  = userModClientAdapter.getUnifiedRepresentationByQuery("query", 1, 2);

    assertEquals(25,unifiedTable.getHeader().size());
    assertEquals(2, unifiedTable.getRows().size());
  }

  @Test
  void getUnifiedRepresentationByQueryIfEmptyResponseTest() {
    var userCollection = new UserCollection();
    userCollection.setUsers(new ArrayList<>());
    when(userClient.getUserByQuery("query", 1, 2)).thenReturn(userCollection);

    var unifiedTable  = userModClientAdapter.getUnifiedRepresentationByQuery("query", 1, 2);

    assertEquals(25,unifiedTable.getHeader().size());
    assertEquals(0, unifiedTable.getRows().size());
  }
}
