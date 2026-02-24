package org.folio.bulkops.client;

import java.net.URI;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import tools.jackson.databind.JsonNode;

@HttpExchange(url = "okapi", accept = MediaType.APPLICATION_JSON_VALUE)
public interface OkapiClient {
  @GetExchange(value = "/proxy/tenants/{tenantId}/modules")
  JsonNode getModuleIds(
      URI uri,
      @PathVariable("tenantId") String tenantId,
      @RequestParam("filter") String moduleName);
}
