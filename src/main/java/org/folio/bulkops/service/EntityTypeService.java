package org.folio.bulkops.service;

import static java.lang.String.format;

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

  public static final UUID FQM_ITEMS_ET_ID =
      UUID.fromString("d0213d22-32cf-490f-9196-d81c3c66e53f");
  public static final UUID FQM_USERS_ET_ID =
      UUID.fromString("ddc93926-d15a-4a45-9d9c-93eadc3d9bbf");
  public static final UUID FQM_HOLDINGS_ET_ID =
      UUID.fromString("8418e512-feac-4a6a-a56d-9006aab31e33");
  public static final UUID FQM_INSTANCES_ET_ID =
      UUID.fromString("6b08439b-4f8e-4468-8046-ea620f5cfb74");
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
                format("Entity type with name=%s is not supported", name));
      };
    } catch (NotFoundException e) {
      log.error("Entity type not found for entityTypeId: {}", entityTypeId, e);
      throw e;
    } catch (Exception e) {
      log.error("Error fetching entity type for entityTypeId: {}", entityTypeId, e);
      throw e;
    }
  }

  public UUID getEntityTypeIdByBulkOpsEntityType(EntityType entityType) {
    return switch (entityType) {
      case ITEM -> FQM_ITEMS_ET_ID;
      case USER -> FQM_USERS_ET_ID;
      case HOLDINGS_RECORD -> FQM_HOLDINGS_ET_ID;
      case INSTANCE, INSTANCE_MARC -> FQM_INSTANCES_ET_ID;
    };
  }
}
