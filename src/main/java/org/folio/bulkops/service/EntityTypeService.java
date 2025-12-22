package org.folio.bulkops.service;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.folio.bulkops.client.EntityTypeClient;
import org.folio.bulkops.domain.dto.EntityType;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EntityTypeService {
  private final EntityTypeClient entityTypeClient;

  public EntityType getBulkOpsEntityTypeByFqmEntityTypeId(UUID entityTypeId) {
    var name = entityTypeClient.getEntityType(entityTypeId).getName();
    return switch (name) {
      case "composite_item_details" -> EntityType.ITEM;
      case "composite_user_details" -> EntityType.USER;
      case "composite_holdings_record" -> EntityType.HOLDINGS_RECORD;
      case "composite_instances" -> EntityType.INSTANCE;
      default ->
          throw new IllegalArgumentException(
              String.format("Entity type with name=%s is not supported", name));
    };
  }

  public UUID getFqmEntityTypeIdByBulkOpsEntityType(EntityType entityType) {
    var label = getLabelByBulkOpsEntityType(entityType);
    return entityTypeClient.getEntityTypeSummaries().getEntityTypes().stream()
        .filter(et -> et.getLabel().equals(label))
        .findFirst()
        .orElseThrow()
        .getId();
  }

  private String getLabelByBulkOpsEntityType(EntityType entityType) {
    return switch (entityType) {
      case ITEM -> "Items";
      case USER -> "Users";
      case HOLDINGS_RECORD -> "Holdings";
      case INSTANCE, INSTANCE_MARC -> "Instances";
    };
  }
}
