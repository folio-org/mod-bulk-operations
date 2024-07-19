package org.folio.bulkops.processor;

import org.apache.commons.lang3.StringUtils;
import org.folio.bulkops.client.ItemClient;
import org.folio.bulkops.domain.bean.ExtendedItem;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.FolioModuleMetadata;
import org.folio.spring.scope.FolioExecutionContextSetter;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

import static org.folio.bulkops.util.FolioExecutionContextUtil.prepareContextForTenant;

@Component
@RequiredArgsConstructor
public class ItemUpdateProcessor extends AbstractUpdateProcessor<ExtendedItem> {

  private final ItemClient itemClient;
  private final FolioModuleMetadata folioModuleMetadata;
  private final FolioExecutionContext folioExecutionContext;

  @Override
  public void updateRecord(ExtendedItem extendedItem) {
    var tenantId = extendedItem.getTenantId();
    var item = extendedItem.getEntity();
    if (StringUtils.isNotEmpty(tenantId)) {
      try (var ignored = new FolioExecutionContextSetter(prepareContextForTenant(tenantId, folioModuleMetadata, folioExecutionContext))) {
        itemClient.updateItem(item.withHoldingsData(null), item.getId());
      }
    } else {
      itemClient.updateItem(item.withHoldingsData(null), item.getId());
    }
  }

  @Override
  public Class<ExtendedItem> getUpdatedType() {
    return ExtendedItem.class;
  }
}
