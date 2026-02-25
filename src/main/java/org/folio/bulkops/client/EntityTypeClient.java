package org.folio.bulkops.client;

import java.util.UUID;
import org.folio.bulkops.domain.bean.EntityTypeSummaries;
import org.folio.querytool.domain.dto.EntityType;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange(url = "entity-types")
public interface EntityTypeClient {
  @GetExchange("/{entityTypeId}")
  EntityType getEntityType(@PathVariable UUID entityTypeId);

  @GetExchange(accept = MediaType.APPLICATION_JSON_VALUE)
  EntityTypeSummaries getEntityTypeSummaries();
}
