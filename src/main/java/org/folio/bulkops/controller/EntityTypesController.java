package org.folio.bulkops.controller;

import lombok.RequiredArgsConstructor;
import org.folio.bulkops.service.EntityTypesService;
import org.folio.querytool.domain.dto.ColumnValues;
import org.folio.querytool.domain.dto.EntityType;
import org.folio.querytool.rest.resource.EntityTypesApi;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RequiredArgsConstructor
@RestController
public class EntityTypesController implements EntityTypesApi {
  private final EntityTypesService entityTypesService;

  @Override
  public ResponseEntity<ColumnValues> getColumnValues(UUID entityTypeId, String columnName, String search) {
    return ResponseEntity.ok(entityTypesService.getColumnValues(entityTypeId, columnName, search));
  }

  @Override
  public ResponseEntity<EntityType> getEntityType(UUID entityTypeId) {
    return ResponseEntity.ok(entityTypesService.getEntityType(entityTypeId));
  }
}
