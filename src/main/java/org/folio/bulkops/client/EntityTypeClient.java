package org.folio.bulkops.client;

import java.util.UUID;
import org.folio.querytool.domain.dto.EntityType;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "entity-types")
public interface EntityTypeClient {
  @GetMapping("/{entityTypeId}")
  EntityType getEntityType(@RequestHeader UUID entityTypeId);
}
