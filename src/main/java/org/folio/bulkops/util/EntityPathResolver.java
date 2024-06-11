package org.folio.bulkops.util;

import lombok.AllArgsConstructor;
import org.folio.bulkops.client.HoldingsClient;
import org.folio.bulkops.domain.bean.BulkOperationsEntity;
import org.folio.bulkops.domain.bean.HoldingsRecord;
import org.folio.bulkops.domain.bean.Instance;
import org.folio.bulkops.domain.bean.Item;
import org.folio.bulkops.domain.bean.User;
import org.folio.bulkops.domain.dto.EntityType;
import org.springframework.stereotype.Component;

import static java.lang.String.format;
import static org.folio.bulkops.domain.dto.IdentifierType.ID;

@Component
@AllArgsConstructor
public class EntityPathResolver {

  private HoldingsClient holdingsClient;

  public String resolve(EntityType type, BulkOperationsEntity entity) {

    switch (type) {
      case USER -> {
        var user = (User) entity;
        return format("/users/%s", user.getId());
      }
      case INSTANCE -> {
        var instance = (Instance) entity;
        return format("/inventory/view/%s", instance.getId());
      }
      case HOLDINGS_RECORD -> {
        var holding = (HoldingsRecord) entity;
        return format("/inventory/view/%s/%s", holding.getInstanceId(), holding.getId());
      }
      case ITEM -> {
        var item = ((Item) entity);
        var holdingId = item.getHoldingsRecordId();
        var holding =   holdingsClient.getHoldingById(holdingId);
        var instanceId = holding.getInstanceId();
        return format("/inventory/view/%s/%s/%s", instanceId, holdingId, item.getId());
      }
    }
    return entity.getIdentifier(ID);
  }
}
