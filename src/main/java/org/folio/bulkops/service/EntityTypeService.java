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

  public EntityType getEntityTypeById(UUID entityTypeId) {
    var name = entityTypeClient.getEntityType(entityTypeId).getName();
    return switch (name) {
      case "composite_item_details" -> EntityType.ITEM;
      case "composite_user_details" -> EntityType.USER;
      case "composite_holdings_record" -> EntityType.HOLDINGS_RECORD;
      case "composite_instances" -> EntityType.INSTANCE;
      default -> throw new IllegalArgumentException(String.format(
              "Entity type with name=%s is not supported", name));
    };
  }
}
