package org.folio.bulkops.processor;

import org.folio.bulkops.client.ItemClient;
import org.folio.bulkops.domain.bean.ExtendedItem;
import org.folio.bulkops.domain.dto.EntityType;
import org.folio.bulkops.processor.permissions.check.PermissionsValidator;
import org.folio.bulkops.service.ConsortiaService;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.FolioModuleMetadata;
import org.folio.spring.scope.FolioExecutionContextSetter;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

import static org.folio.bulkops.util.FolioExecutionContextUtil.prepareContextForTenant;

@Component
@RequiredArgsConstructor
public class ItemUpdateProcessor extends AbstractUpdateProcessor<ExtendedItem> {

  private static final String NO_ITEM_WRITE_PERMISSIONS_TEMPLATE = "User %s does not have required permission to edit the item record - %s=%s on the tenant ";

  private final ItemClient itemClient;
  private final FolioModuleMetadata folioModuleMetadata;
  private final FolioExecutionContext folioExecutionContext;
  private final ConsortiaService consortiaService;
  private final PermissionsValidator permissionsValidator;

  @Override
  public void updateRecord(ExtendedItem extendedItem) {
    var item = extendedItem.getEntity();
    item.setTenant(null);
    if (consortiaService.isCurrentTenantCentralTenant(folioExecutionContext.getTenantId())) {
      var tenantId = extendedItem.getTenantId();
      permissionsValidator.checkIfBulkEditWritePermissionExists(tenantId, EntityType.ITEM,
        NO_ITEM_WRITE_PERMISSIONS_TEMPLATE + tenantId);
      try (var ignored = new FolioExecutionContextSetter(prepareContextForTenant(tenantId, folioModuleMetadata, folioExecutionContext))) {
        itemClient.updateItem(item.withHoldingsData(null), item.getId());
      }
    } else {
      permissionsValidator.checkIfBulkEditWritePermissionExists(folioExecutionContext.getTenantId(), EntityType.ITEM,
        NO_ITEM_WRITE_PERMISSIONS_TEMPLATE + folioExecutionContext.getTenantId());
      itemClient.updateItem(item.withHoldingsData(null), item.getId());
    }
  }

  @Override
  public Class<ExtendedItem> getUpdatedType() {
    return ExtendedItem.class;
  }
}
