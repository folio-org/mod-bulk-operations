package org.folio.bulkops.service;

import lombok.RequiredArgsConstructor;
import org.folio.bulkops.domain.dto.EntityTypeSummary;
import org.folio.bulkops.client.EntityTypesClient;
import org.folio.querytool.domain.dto.ColumnValues;
import org.folio.querytool.domain.dto.EntityType;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class EntityTypesService {

  private final EntityTypesClient entityTypesClient;

  public List<EntityTypeSummary> getEntityTypeSummary(List<UUID> ids) {
    return entityTypesClient.getEntityTypeSummary(ids);
  }

  public ColumnValues getColumnValues(UUID entityTypeId, String columnName, String search) {
    return entityTypesClient.getColumnValues(entityTypeId, columnName, search);
  }

  public EntityType getEntityType(UUID entityTypeId) {
    return entityTypesClient.getEntityType(entityTypeId);
  }
}
