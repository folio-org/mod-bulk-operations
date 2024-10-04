package org.folio.bulkops.processor.permissions.check;


import static org.folio.bulkops.processor.permissions.check.PermissionsValidator.BULK_EDIT_INVENTORY_WRITE_PERMISSION;
import static org.folio.bulkops.processor.permissions.check.PermissionsValidator.BULK_EDIT_USERS_WRITE_PERMISSION;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.folio.bulkops.domain.dto.EntityType;
import org.folio.bulkops.exception.WritePermissionDoesNotExist;
import org.folio.spring.FolioExecutionContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PermissionsValidatorTest {

  @Mock
  private PermissionsProvider permissionsProvider;
  @Mock
  private RequiredPermissionResolver requiredPermissionResolver;
  @Mock
  private FolioExecutionContext folioExecutionContext;

  @InjectMocks
  private PermissionsValidator permissionsValidator;

  @Test
  void testCheckIfBulkEditWritePermissionExistsForInventory() {
    when(permissionsProvider.getUserPermissions(eq("tenant1"), isA(UUID.class))).thenReturn(List.of("write_permission", "not_write_permission", BULK_EDIT_INVENTORY_WRITE_PERMISSION));
    when(permissionsProvider.getUserPermissions(eq("tenant2"), isA(UUID.class))).thenReturn(List.of("not_write_permission"));
    when(requiredPermissionResolver.getWritePermission(EntityType.ITEM)).thenReturn("write_permission");
    when(folioExecutionContext.getUserId()).thenReturn(UUID.randomUUID());

    Assertions.assertDoesNotThrow(() -> permissionsValidator.checkIfBulkEditWritePermissionExists("tenant1", EntityType.ITEM, "errorMessage"));
    Assertions.assertThrows(WritePermissionDoesNotExist.class, () -> permissionsValidator.checkIfBulkEditWritePermissionExists("tenant2", EntityType.ITEM, "errorMessage"));
  }

  @Test
  void testCheckIfBulkEditWritePermissionExistsForUsers() {
    when(permissionsProvider.getUserPermissions(eq("tenant1"),  isA(UUID.class))).thenReturn(List.of("write_permission", "not_write_permission", BULK_EDIT_USERS_WRITE_PERMISSION));
    when(permissionsProvider.getUserPermissions(eq("tenant2"), isA(UUID.class))).thenReturn(List.of("not_write_permission"));
    when(requiredPermissionResolver.getWritePermission(EntityType.USER)).thenReturn("write_permission");
    when(folioExecutionContext.getUserId()).thenReturn(UUID.randomUUID());

    Assertions.assertDoesNotThrow(() -> permissionsValidator.checkIfBulkEditWritePermissionExists("tenant1", EntityType.USER, "errorMessage"));
    Assertions.assertThrows(WritePermissionDoesNotExist.class, () -> permissionsValidator.checkIfBulkEditWritePermissionExists("tenant2", EntityType.USER, "errorMessage"));
  }
}
