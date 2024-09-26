package org.folio.bulkops.processor.permissions.check;

import org.folio.bulkops.domain.dto.EntityType;
import org.folio.bulkops.exception.WritePermissionDoesNotExist;
import org.folio.spring.FolioExecutionContext;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Component
@RequiredArgsConstructor
@Log4j2
public class PermissionsValidator {

  public static final String BULK_EDIT_INVENTORY_WRITE_PERMISSION = "bulk-operations.item.inventory.put";
  public static final String BULK_EDIT_USERS_WRITE_PERMISSION = "bulk-operations.item.users.put";

  private final PermissionsProvider permissionsProvider;
  private final RequiredPermissionResolver requiredPermissionResolver;
  private final FolioExecutionContext folioExecutionContext;

  public void checkIfBulkEditWritePermissionExists(String tenantId, EntityType entityType, String errorMessage) {
    if (!isBulkEditWritePermissionExists(tenantId, entityType)) {
      throw new WritePermissionDoesNotExist(errorMessage);
    }
  }

  private boolean isBulkEditWritePermissionExists(String tenantId, EntityType entityType) {
    var readPermissionForEntity = requiredPermissionResolver.getWritePermission(entityType);
    var userPermissions = permissionsProvider.getUserPermissions(tenantId);
    var isWritePermissionsExist = false;
    if (entityType == EntityType.USER) {
      isWritePermissionsExist = userPermissions.contains(readPermissionForEntity) && userPermissions.contains(BULK_EDIT_USERS_WRITE_PERMISSION);
    } else {
      isWritePermissionsExist = userPermissions.contains(readPermissionForEntity) && userPermissions.contains(BULK_EDIT_INVENTORY_WRITE_PERMISSION);
    }
    log.info("isBulkEditWritePermissionExists:: user {} has write permissions {} for {} in tenant {}", folioExecutionContext.getUserId(),
      isWritePermissionsExist, entityType, tenantId);
    return isWritePermissionsExist;
  }

}
