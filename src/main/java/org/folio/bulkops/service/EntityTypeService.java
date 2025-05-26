package org.folio.bulkops.service;

import static org.folio.bulkops.util.Constants.HOLDINGS_ENTITY_TYPE_ID;
import static org.folio.bulkops.util.Constants.INSTANCE_ENTITY_TYPE_ID;
import static org.folio.bulkops.util.Constants.ITEM_ENTITY_TYPE_ID;
import static org.folio.bulkops.util.Constants.USER_ENTITY_TYPE_ID;

import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.folio.bulkops.client.EntityTypeClient;
import org.folio.bulkops.domain.dto.EntityType;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EntityTypeService {
  private final EntityTypeClient entityTypeClient;

  public EntityType getEntityTypeById(UUID entityTypeId) {
    var id = entityTypeClient.getEntityType(entityTypeId).getId();
    var alias = entityTypeClient.getEntityType(entityTypeId).getLabelAlias();
    return switch (alias) {
      case "Items" -> EntityType.ITEM;
      case "Users" -> EntityType.USER;
      case "Holdings" -> EntityType.HOLDINGS_RECORD;
      case "Instances" -> EntityType.INSTANCE;
      default -> getAliasOrId(id, alias);
    };
  }

  private EntityType getAliasOrId(String id, String alias) {
    return switch (id) {
      case ITEM_ENTITY_TYPE_ID -> EntityType.ITEM;
      case USER_ENTITY_TYPE_ID -> EntityType.USER;
      case HOLDINGS_ENTITY_TYPE_ID -> EntityType.HOLDINGS_RECORD;
      case INSTANCE_ENTITY_TYPE_ID -> EntityType.INSTANCE;
      default -> throw new IllegalArgumentException(String.format("Entity type id=%s and alias=%s is not supported", id, alias));
    };
  }
}
