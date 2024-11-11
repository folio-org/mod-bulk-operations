package org.folio.bulkops.util;

import lombok.AllArgsConstructor;
import org.folio.bulkops.client.HoldingsClient;
import org.folio.bulkops.domain.bean.BulkOperationsEntity;
import org.folio.bulkops.domain.bean.HoldingsRecord;
import org.folio.bulkops.domain.bean.Instance;
import org.folio.bulkops.domain.bean.Item;
import org.folio.bulkops.domain.bean.User;
import org.folio.bulkops.domain.dto.EntityType;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.FolioModuleMetadata;
import org.folio.spring.scope.FolioExecutionContextSetter;
import org.springframework.stereotype.Component;

import static java.lang.String.format;
import static org.folio.bulkops.domain.dto.IdentifierType.ID;
import static org.folio.bulkops.util.FolioExecutionContextUtil.prepareContextForTenant;

@Component
@AllArgsConstructor
public class EntityPathResolver {

  private HoldingsClient holdingsClient;
  private final FolioModuleMetadata folioModuleMetadata;
  private final FolioExecutionContext folioExecutionContext;

  public String resolve(EntityType type, BulkOperationsEntity entity) {
    var recordEntity = entity.getRecordBulkOperationEntity();
    switch (type) {
      case USER -> {
        var user = (User) recordEntity;
        return format("/users/%s", user.getId());
      }
      case INSTANCE -> {
        var instance = (Instance) recordEntity;
        return format("/inventory/view/%s", instance.getId());
      }
      case HOLDINGS_RECORD -> {
        var holding = (HoldingsRecord) recordEntity;
        return format("/inventory/view/%s/%s", holding.getInstanceId(), holding.getId());
      }
      case ITEM -> {
        var tenantIdOfEntity = entity.getTenant();
        var item = (Item) recordEntity;
        var holdingId = item.getHoldingsRecordId();
        HoldingsRecord holding;
        try (var ignored = new FolioExecutionContextSetter(prepareContextForTenant(tenantIdOfEntity, folioModuleMetadata, folioExecutionContext))) {
          holding = holdingsClient.getHoldingById(holdingId);
        }
        var instanceId = holding.getInstanceId();
        return format("/inventory/view/%s/%s/%s", instanceId, holdingId, item.getId());
      }
    }
    return recordEntity.getIdentifier(ID);
  }
}
