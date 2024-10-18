package org.folio.bulkops.processor.permissions.check;

import org.folio.bulkops.domain.dto.EntityType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RequiredPermissionResolverTest {

  @Test
  void testGetWritePermission() {
    var requiredPermissionResolver = new RequiredPermissionResolver();
    assertEquals("users.item.put",  requiredPermissionResolver.getWritePermission(EntityType.USER).getValue());
    assertEquals("inventory.items.item.put",  requiredPermissionResolver.getWritePermission(EntityType.ITEM).getValue());
    assertEquals("inventory-storage.holdings.item.put",  requiredPermissionResolver.getWritePermission(EntityType.HOLDINGS_RECORD).getValue());
    assertEquals("inventory.instances.item.put",  requiredPermissionResolver.getWritePermission(EntityType.INSTANCE).getValue());
  }
}
