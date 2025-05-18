package org.folio.bulkops.processor.permissions.check;

import static org.folio.bulkops.domain.dto.EntityType.HOLDINGS_RECORD;
import static org.folio.bulkops.domain.dto.EntityType.INSTANCE;
import static org.folio.bulkops.domain.dto.EntityType.INSTANCE_MARC;
import static org.folio.bulkops.domain.dto.EntityType.ITEM;
import static org.folio.bulkops.domain.dto.EntityType.USER;
import static org.folio.bulkops.processor.permissions.check.PermissionEnum.BULK_EDIT_INVENTORY_VIEW_PERMISSION;
import static org.folio.bulkops.processor.permissions.check.PermissionEnum.BULK_EDIT_INVENTORY_WRITE_PERMISSION;
import static org.folio.bulkops.processor.permissions.check.PermissionEnum.BULK_EDIT_USERS_VIEW_PERMISSION;
import static org.folio.bulkops.processor.permissions.check.PermissionEnum.BULK_EDIT_USERS_WRITE_PERMISSION;
import static org.folio.bulkops.util.Constants.LINKED_DATA_SOURCE;
import static org.folio.bulkops.util.Constants.LINKED_DATA_SOURCE_IS_NOT_SUPPORTED;
import static org.folio.bulkops.util.Constants.NO_INSTANCE_VIEW_PERMISSIONS;
import static org.folio.bulkops.util.Constants.NO_USER_VIEW_PERMISSIONS;

import lombok.RequiredArgsConstructor;
import org.folio.bulkops.client.UserClient;
import org.folio.bulkops.domain.bean.BulkOperationsEntity;
import org.folio.bulkops.domain.bean.User;
import org.folio.bulkops.domain.dto.EntityType;
import org.folio.bulkops.domain.entity.BulkOperation;
import org.folio.bulkops.exception.ReadPermissionException;
import org.folio.bulkops.exception.UploadFromQueryException;
import org.folio.bulkops.exception.WritePermissionDoesNotExist;
import org.folio.spring.FolioExecutionContext;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@RequiredArgsConstructor
public class PermissionsValidator {

  private final PermissionsProvider permissionsProvider;
  private final RequiredPermissionResolver requiredPermissionResolver;
  private final FolioExecutionContext folioExecutionContext;
  private final UserClient userClient;
  private final ItemPermissionChecker itemPermissionChecker;
  private final HoldingsRecordPermissionChecker holdingsRecordPermissionChecker;

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

  public void checkPermissions(BulkOperation operation, BulkOperationsEntity record) throws UploadFromQueryException {
    if (Set.of(USER, INSTANCE, INSTANCE_MARC).contains(operation.getEntityType())) {
      if (!isBulkEditReadPermissionExists(folioExecutionContext.getTenantId(), operation.getEntityType())) {
        var user = userClient.getUserById(folioExecutionContext.getUserId().toString());
        throw new ReadPermissionException(buildReadPermissionErrorMessage(operation, record.getId(), user), record.getId());
      }
      if (LINKED_DATA_SOURCE.equals(record.getSource())) {
        throw new UploadFromQueryException(LINKED_DATA_SOURCE_IS_NOT_SUPPORTED, record.getId());
      }
    } else if (HOLDINGS_RECORD == operation.getEntityType()) {
      holdingsRecordPermissionChecker.checkPermissionsAndAffiliations(record.getId());
    } else if (ITEM == operation.getEntityType()) {
      itemPermissionChecker.checkPermissionsAndAffiliations(record.getId());
    }
  }

  public boolean isBulkEditReadPermissionExists(String tenantId, EntityType entityType) {
    var readPermissionForEntity = requiredPermissionResolver.getReadPermission(entityType);
    var userPermissions = permissionsProvider.getUserPermissions(tenantId, folioExecutionContext.getUserId());
    var isReadPermissionsExist = false;
    if (entityType == EntityType.USER) {
      isReadPermissionsExist = userPermissions.contains(readPermissionForEntity.getValue()) && userPermissions.contains(BULK_EDIT_USERS_VIEW_PERMISSION.getValue());
    } else {
      isReadPermissionsExist = userPermissions.contains(readPermissionForEntity.getValue()) && userPermissions.contains(BULK_EDIT_INVENTORY_VIEW_PERMISSION.getValue());
    }
    return isReadPermissionsExist;
  }

  private String buildReadPermissionErrorMessage(BulkOperation operation, String identifier, User user) {
    return switch (operation.getEntityType()) {
      case USER -> NO_USER_VIEW_PERMISSIONS.formatted(user.getUsername(), "id", identifier, folioExecutionContext.getTenantId());
      case INSTANCE, INSTANCE_MARC -> NO_INSTANCE_VIEW_PERMISSIONS.formatted(user.getUsername(), "id", identifier, folioExecutionContext.getTenantId());
      default -> throw new IllegalArgumentException("For %s this error message builder cannot be used.".formatted(operation.getEntityType()));
    };
  }

}
