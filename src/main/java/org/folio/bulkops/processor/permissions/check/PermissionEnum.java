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
  INVENTORY_INSTANCES_ITEM_PUT("inventory.instances.item.put"),
  BULK_EDIT_INVENTORY_VIEW_PERMISSION("bulk-operations.item.inventory.get"),
  BULK_EDIT_USERS_VIEW_PERMISSION("bulk-operations.item.users.get"),
  USER_ITEM_GET_PERMISSION("users.item.get"),
  INVENTORY_ITEMS_ITEM_GET_PERMISSION("inventory.items.item.get"),
  INVENTORY_STORAGE_HOLDINGS_ITEM_GET_PERMISSION("inventory-storage.holdings.item.get"),
  INVENTORY_INSTANCES_ITEM_GET_PERMISSION("inventory.instances.item.get");

  private final String value;
}
