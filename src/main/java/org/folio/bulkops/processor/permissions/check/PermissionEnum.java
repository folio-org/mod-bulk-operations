package org.folio.bulkops.processor.permissions.check;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum PermissionEnum {
  BULK_EDIT_INVENTORY_WRITE_PERMISSION("bulk-operations.item.inventory.put"),
  BULK_EDIT_USERS_WRITE_PERMISSION("bulk-operations.item.users.put"),
  USERS_ITEM_PUT("users.item.put"),
  INVENTORY_ITEMS_ITEM_PUT("inventory.items.item.put"),
  INVENTORY_STORAGE_HOLDINGS_ITEM_PUT("inventory-storage.holdings.item.put"),
  INVENTORY_INSTANCES_ITEM_PUT("inventory.instances.item.put");

  private final String value;
}
