package org.folio.bulkops.client;

import java.net.URI;
import org.folio.bulkops.configs.FeignClientConfiguration;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import tools.jackson.databind.JsonNode;

@FeignClient(name = "okapi", configuration = FeignClientConfiguration.class)
public interface OkapiClient {
  @GetMapping(
      value = "/proxy/tenants/{tenantId}/modules",
      produces = MediaType.APPLICATION_JSON_VALUE)
  JsonNode getModuleIds(
      URI uri,
      @PathVariable("tenantId") String tenantId,
      @RequestParam("filter") String moduleName);
}
