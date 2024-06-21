package org.folio.bulkops.service;

import lombok.RequiredArgsConstructor;
import org.folio.bulkops.client.EntityTypeClient;
import org.folio.bulkops.domain.dto.EntityType;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EntityTypeService {
  private final EntityTypeClient entityTypeClient;

  public EntityType getEntityTypeById(UUID entityTypeId) {
    var alias = entityTypeClient.getEntityType(entityTypeId).getLabelAlias();
    return switch (alias) {
      case "Items" -> EntityType.ITEM;
      case "Users" -> EntityType.USER;
      case "Holdings" -> EntityType.HOLDINGS_RECORD;
      case "Instances" -> EntityType.INSTANCE_FOLIO;
      default -> throw new IllegalArgumentException(String.format("Entity type %s is not supported", alias));
    };
  }
}
