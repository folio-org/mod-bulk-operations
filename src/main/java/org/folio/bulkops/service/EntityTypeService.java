package org.folio.bulkops.service;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.bulkops.client.EntityTypeClient;
import org.folio.bulkops.domain.dto.EntityType;
import org.folio.bulkops.exception.NotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Log4j2
public class EntityTypeService {
  private final EntityTypeClient entityTypeClient;

  public EntityType getBulkOpsEntityTypeByFqmEntityTypeId(UUID entityTypeId) {
    try {
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
    } catch (NotFoundException e) {
      log.error("Entity type not found for entityTypeId: {}", entityTypeId, e);
      throw e;
    } catch (Exception e) {
      log.error("Error fetching entity type for entityTypeId: {}", entityTypeId, e);
      throw new NotFoundException(
          String.format("Unable to retrieve entity type for id: %s", entityTypeId));
    }
  }

  public UUID getFqmEntityTypeIdByBulkOpsEntityType(EntityType entityType) {
    try {
      var label = getLabelByBulkOpsEntityType(entityType);
      return entityTypeClient.getEntityTypeSummaries().getEntityTypes().stream()
          .filter(et -> et.getLabel().equals(label))
          .findFirst()
          .orElseThrow(
              () ->
                  new NotFoundException(
                      String.format(
                          "Entity type not found for label: %s (EntityType: %s)",
                          label, entityType)))
          .getId();
    } catch (NotFoundException e) {
      log.error("Entity type not found for entityType: {}", entityType, e);
      throw e;
    } catch (Exception e) {
      log.error("Error fetching entity type summaries for entityType: {}", entityType, e);
      throw new NotFoundException(
          String.format("Unable to retrieve entity type summaries for type: %s", entityType));
    }
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
