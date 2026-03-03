package org.folio.bulkops.client;

import static org.folio.bulkops.util.Constants.OKAPI_URL;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import tools.jackson.databind.JsonNode;

@HttpExchange(url = OKAPI_URL, accept = MediaType.APPLICATION_JSON_VALUE)
public interface OkapiClient {
  @GetExchange(value = "/proxy/tenants/{tenantId}/modules")
  JsonNode getModuleIds(
      @PathVariable("tenantId") String tenantId, @RequestParam("filter") String moduleName);
}
