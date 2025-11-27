package org.folio.bulkops.processor.permissions.check;

import static org.folio.bulkops.processor.permissions.check.PermissionEnum.BULK_EDIT_INVENTORY_VIEW_PERMISSION;
import static org.folio.bulkops.processor.permissions.check.PermissionEnum.BULK_EDIT_USERS_VIEW_PERMISSION;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.spring.FolioExecutionContext;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Log4j2
public class ReadPermissionsValidator {

  private final PermissionsProvider permissionsProvider;
  private final RequiredPermissionResolver requiredPermissionResolver;
  private final FolioExecutionContext folioExecutionContext;

  public boolean isBulkEditReadPermissionExists(
      String tenantId, org.folio.bulkops.domain.dto.EntityType entityType) {
    var readPermissionForEntity = requiredPermissionResolver.getReadPermission(entityType);
    var userPermissions =
        permissionsProvider.getUserPermissions(tenantId, folioExecutionContext.getUserId());
    var isReadPermissionsExist = false;
    if (entityType == org.folio.bulkops.domain.dto.EntityType.USER) {
      isReadPermissionsExist =
          userPermissions.contains(readPermissionForEntity.getValue())
              && userPermissions.contains(BULK_EDIT_USERS_VIEW_PERMISSION.getValue());
    } else {
      isReadPermissionsExist =
          userPermissions.contains(readPermissionForEntity.getValue())
              && userPermissions.contains(BULK_EDIT_INVENTORY_VIEW_PERMISSION.getValue());
    }
    return isReadPermissionsExist;
  }
}
