package org.folio.bulkops.client;

import org.folio.bulkops.configs.FeignClientConfiguration;
import org.folio.bulkops.domain.dto.EntityTypeSummary;
import org.folio.querytool.domain.dto.ColumnValues;
import org.folio.querytool.domain.dto.EntityType;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.UUID;

@FeignClient(name = "entity-types", configuration = FeignClientConfiguration.class)
public interface EntityTypesClient {
  @GetMapping
  List<EntityTypeSummary> getEntityTypeSummary(@RequestParam List<UUID> ids);

  @GetMapping(path = "/{entityTypeId}/columns/{columnName}/values")
  ColumnValues getColumnValues(@PathVariable UUID entityTypeId,
                               @PathVariable String columnName,
                               @RequestParam String search);

  @GetMapping(path = "/{entityTypeId}")
  EntityType getEntityType(@PathVariable UUID entityTypeId);
}
