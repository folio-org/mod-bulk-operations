package org.folio.bulkops.processor.permissions.check;

import org.folio.bulkops.domain.dto.EntityType;
import org.springframework.stereotype.Component;

@Component
public class RequiredPermissionResolver {

  public String getWritePermission(EntityType entityType) {
    return switch (entityType) {
      case USER -> "users.item.put";
      case ITEM -> "inventory.items.item.put";
      case HOLDINGS_RECORD -> "inventory-storage.holdings.item.put";
      case INSTANCE, INSTANCE_MARC -> "inventory.instances.item.put";
    };
  }
}
