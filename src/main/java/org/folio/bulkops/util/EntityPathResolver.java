package org.folio.bulkops.util;

import static java.lang.String.format;
import static org.folio.bulkops.domain.dto.IdentifierType.ID;
import static org.folio.bulkops.util.FolioExecutionContextUtil.prepareContextForTenant;

import lombok.AllArgsConstructor;
import org.folio.bulkops.client.HoldingsStorageClient;
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

@Component
@AllArgsConstructor
public class EntityPathResolver {

  private HoldingsStorageClient holdingsStorageClient;
  private final FolioModuleMetadata folioModuleMetadata;
  private final FolioExecutionContext folioExecutionContext;

  public String resolve(EntityType type, BulkOperationsEntity entity) {
    var recordEntity = entity.getRecordBulkOperationEntity();
    switch (type) {
      case USER -> {
        var user = (User) recordEntity;
        return format("/users/%s", user.getId());
      }
      case INSTANCE, INSTANCE_MARC -> {
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
        try (var ignored =
            new FolioExecutionContextSetter(
                prepareContextForTenant(
                    tenantIdOfEntity, folioModuleMetadata, folioExecutionContext))) {
          holding = holdingsStorageClient.getHoldingById(holdingId);
        }
        var instanceId = holding.getInstanceId();
        return format("/inventory/view/%s/%s/%s", instanceId, holdingId, item.getId());
      }
      default -> {
        return recordEntity.getIdentifier(ID);
      }
    }
  }
}
