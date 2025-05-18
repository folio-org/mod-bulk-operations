package org.folio.bulkops.processor.permissions.check;

import org.folio.bulkops.domain.dto.EntityType;
import org.folio.bulkops.exception.WritePermissionDoesNotExist;
import org.folio.spring.FolioExecutionContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.folio.bulkops.processor.permissions.check.PermissionEnum.BULK_EDIT_INVENTORY_VIEW_PERMISSION;
import static org.folio.bulkops.processor.permissions.check.PermissionEnum.BULK_EDIT_INVENTORY_WRITE_PERMISSION;
import static org.folio.bulkops.processor.permissions.check.PermissionEnum.BULK_EDIT_USERS_VIEW_PERMISSION;
import static org.folio.bulkops.processor.permissions.check.PermissionEnum.BULK_EDIT_USERS_WRITE_PERMISSION;
import static org.folio.bulkops.processor.permissions.check.PermissionEnum.INVENTORY_INSTANCES_ITEM_GET_PERMISSION;
import static org.folio.bulkops.processor.permissions.check.PermissionEnum.INVENTORY_ITEMS_ITEM_PUT;
import static org.folio.bulkops.processor.permissions.check.PermissionEnum.INVENTORY_STORAGE_HOLDINGS_ITEM_GET_PERMISSION;
import static org.folio.bulkops.processor.permissions.check.PermissionEnum.USERS_ITEM_PUT;
import static org.folio.bulkops.processor.permissions.check.PermissionEnum.USER_ITEM_GET_PERMISSION;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PermissionsValidatorTest {

  @Mock
  private PermissionsProvider permissionsProvider;
  @Mock
  private FolioExecutionContext folioExecutionContext;
  @Spy
  private RequiredPermissionResolver requiredPermissionResolver;


  @InjectMocks
  private PermissionsValidator permissionsValidator;

  @Test
  void testCheckIfBulkEditWritePermissionExistsForInventory() {
    when(permissionsProvider.getUserPermissions(eq("tenant1"), isA(UUID.class))).thenReturn(List.of(INVENTORY_ITEMS_ITEM_PUT.getValue(), "not_write_permission", BULK_EDIT_INVENTORY_WRITE_PERMISSION.getValue()));
    when(permissionsProvider.getUserPermissions(eq("tenant2"), isA(UUID.class))).thenReturn(List.of("not_write_permission"));
    when(folioExecutionContext.getUserId()).thenReturn(UUID.randomUUID());

    Assertions.assertDoesNotThrow(() -> permissionsValidator.checkIfBulkEditWritePermissionExists("tenant1", EntityType.ITEM, "errorMessage"));
    Assertions.assertThrows(WritePermissionDoesNotExist.class, () -> permissionsValidator.checkIfBulkEditWritePermissionExists("tenant2", EntityType.ITEM, "errorMessage"));
  }

  @Test
  void testCheckIfBulkEditWritePermissionExistsForUsers() {
    when(permissionsProvider.getUserPermissions(eq("tenant1"),  isA(UUID.class))).thenReturn(List.of(USERS_ITEM_PUT.getValue(), "not_write_permission", BULK_EDIT_USERS_WRITE_PERMISSION.getValue()));
    when(permissionsProvider.getUserPermissions(eq("tenant2"), isA(UUID.class))).thenReturn(List.of("not_write_permission"));
    when(folioExecutionContext.getUserId()).thenReturn(UUID.randomUUID());

    Assertions.assertDoesNotThrow(() -> permissionsValidator.checkIfBulkEditWritePermissionExists("tenant1", EntityType.USER, "errorMessage"));
    Assertions.assertThrows(WritePermissionDoesNotExist.class, () -> permissionsValidator.checkIfBulkEditWritePermissionExists("tenant2", EntityType.USER, "errorMessage"));
  }

  @Test
  void testCheckIfBulkEditReadPermissionExistsForUsers() {
    when(permissionsProvider.getUserPermissions(eq("tenant1"),  isA(UUID.class))).thenReturn(List.of(BULK_EDIT_INVENTORY_VIEW_PERMISSION.getValue()));
    when(permissionsProvider.getUserPermissions(eq("tenant2"), isA(UUID.class))).thenReturn(List.of(BULK_EDIT_USERS_VIEW_PERMISSION.getValue(), USER_ITEM_GET_PERMISSION.getValue()));
    when(folioExecutionContext.getUserId()).thenReturn(UUID.randomUUID());

    assertFalse(permissionsValidator.isBulkEditReadPermissionExists("tenant1", EntityType.USER));
    assertTrue(permissionsValidator.isBulkEditReadPermissionExists("tenant2", EntityType.USER));
  }

  @ParameterizedTest
  @EnumSource(value = EntityType.class, names = {"INSTANCE", "INSTANCE_MARC"})
  void testCheckIfBulkEditReadPermissionExistsForInstance(EntityType entityType) {
    when(permissionsProvider.getUserPermissions(eq("tenant1"),  isA(UUID.class))).thenReturn(List.of(BULK_EDIT_USERS_VIEW_PERMISSION.getValue()));
    when(permissionsProvider.getUserPermissions(eq("tenant2"), isA(UUID.class))).thenReturn(List.of(BULK_EDIT_INVENTORY_VIEW_PERMISSION.getValue(), INVENTORY_INSTANCES_ITEM_GET_PERMISSION.getValue()));
    when(folioExecutionContext.getUserId()).thenReturn(UUID.randomUUID());

    assertFalse(permissionsValidator.isBulkEditReadPermissionExists("tenant1", entityType));
    assertTrue(permissionsValidator.isBulkEditReadPermissionExists("tenant2", entityType));
  }

  @Test
  void testCheckIfBulkEditReadPermissionExistsForHoldings() {
    when(permissionsProvider.getUserPermissions(eq("tenant1"),  isA(UUID.class))).thenReturn(List.of(BULK_EDIT_USERS_VIEW_PERMISSION.getValue()));
    when(permissionsProvider.getUserPermissions(eq("tenant2"), isA(UUID.class))).thenReturn(List.of(BULK_EDIT_INVENTORY_VIEW_PERMISSION.getValue(), INVENTORY_STORAGE_HOLDINGS_ITEM_GET_PERMISSION.getValue()));
    when(folioExecutionContext.getUserId()).thenReturn(UUID.randomUUID());

    assertFalse(permissionsValidator.isBulkEditReadPermissionExists("tenant1", EntityType.HOLDINGS_RECORD));
    assertTrue(permissionsValidator.isBulkEditReadPermissionExists("tenant2", EntityType.HOLDINGS_RECORD));
  }
}
