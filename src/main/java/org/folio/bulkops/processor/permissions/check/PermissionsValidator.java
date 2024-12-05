package org.folio.bulkops.processor.permissions.check;

import org.folio.bulkops.domain.dto.EntityType;
import org.folio.bulkops.exception.WritePermissionDoesNotExist;
import org.folio.spring.FolioExecutionContext;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

import static org.folio.bulkops.processor.permissions.check.PermissionEnum.BULK_EDIT_INVENTORY_WRITE_PERMISSION;
import static org.folio.bulkops.processor.permissions.check.PermissionEnum.BULK_EDIT_USERS_WRITE_PERMISSION;

@Component
@RequiredArgsConstructor
public class PermissionsValidator {

  private final PermissionsProvider permissionsProvider;
  private final RequiredPermissionResolver requiredPermissionResolver;
  private final FolioExecutionContext folioExecutionContext;

  public void checkIfBulkEditWritePermissionExists(String tenantId, EntityType entityType, String errorMessage) {
    if (!isBulkEditWritePermissionExists(tenantId, entityType)) {
      throw new WritePermissionDoesNotExist(errorMessage);
    }
  }

  private boolean isBulkEditWritePermissionExists(String tenantId, EntityType entityType) {
    var writePermissionForEntity = requiredPermissionResolver.getWritePermission(entityType);
    var userPermissions = permissionsProvider.getUserPermissions(tenantId, folioExecutionContext.getUserId());
    var isWritePermissionsExist = false;
    if (entityType == EntityType.USER) {
      isWritePermissionsExist = userPermissions.contains(writePermissionForEntity.getValue()) && userPermissions.contains(BULK_EDIT_USERS_WRITE_PERMISSION.getValue());
    } else {
      isWritePermissionsExist = userPermissions.contains(writePermissionForEntity.getValue()) && userPermissions.contains(BULK_EDIT_INVENTORY_WRITE_PERMISSION.getValue());
    }
    return isWritePermissionsExist;
  }

}
