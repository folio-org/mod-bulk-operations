package org.folio.bulkops.processor.permissions.check;

import org.folio.bulkops.domain.dto.EntityType;
import org.springframework.stereotype.Component;

import static org.folio.bulkops.processor.permissions.check.PermissionEnum.INVENTORY_INSTANCES_ITEM_GET_PERMISSION;
import static org.folio.bulkops.processor.permissions.check.PermissionEnum.INVENTORY_INSTANCES_ITEM_PUT;
import static org.folio.bulkops.processor.permissions.check.PermissionEnum.INVENTORY_ITEMS_ITEM_GET_PERMISSION;
import static org.folio.bulkops.processor.permissions.check.PermissionEnum.INVENTORY_ITEMS_ITEM_PUT;
import static org.folio.bulkops.processor.permissions.check.PermissionEnum.INVENTORY_STORAGE_HOLDINGS_ITEM_GET_PERMISSION;
import static org.folio.bulkops.processor.permissions.check.PermissionEnum.INVENTORY_STORAGE_HOLDINGS_ITEM_PUT;
import static org.folio.bulkops.processor.permissions.check.PermissionEnum.USERS_ITEM_PUT;
import static org.folio.bulkops.processor.permissions.check.PermissionEnum.USER_ITEM_GET_PERMISSION;

@Component
public class RequiredPermissionResolver {

  public PermissionEnum getWritePermission(EntityType entityType) {
    return switch (entityType) {
      case USER -> USERS_ITEM_PUT;
      case ITEM -> INVENTORY_ITEMS_ITEM_PUT;
      case HOLDINGS_RECORD -> INVENTORY_STORAGE_HOLDINGS_ITEM_PUT;
      case INSTANCE, INSTANCE_MARC -> INVENTORY_INSTANCES_ITEM_PUT;
    };
  }

  public PermissionEnum getReadPermission(EntityType entityType) {
    return switch (entityType) {
      case USER -> USER_ITEM_GET_PERMISSION;
      case ITEM -> INVENTORY_ITEMS_ITEM_GET_PERMISSION;
      case HOLDINGS_RECORD -> INVENTORY_STORAGE_HOLDINGS_ITEM_GET_PERMISSION;
      case INSTANCE, INSTANCE_MARC -> INVENTORY_INSTANCES_ITEM_GET_PERMISSION;
    };
  }
}
