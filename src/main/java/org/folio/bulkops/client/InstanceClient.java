package org.folio.bulkops.client;

import io.swagger.v3.oas.annotations.parameters.RequestBody;
import org.folio.bulkops.domain.bean.BriefInstanceCollection;
import org.folio.bulkops.domain.bean.InstanceCollection;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PatchExchange;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

@HttpExchange(url = "inventory/instances", accept = MediaType.APPLICATION_JSON_VALUE)
public interface InstanceClient {
  @PatchExchange(value = "/{instanceId}")
  void patchInstance(@RequestBody ObjectNode patch, @PathVariable String instanceId);

  @GetExchange
  BriefInstanceCollection getByQuery(@RequestParam String query);

  @GetExchange
  InstanceCollection getInstanceByQuery(
      @RequestParam("query") String query, @RequestParam long limit);

  @GetExchange(value = "/{instanceId}")
  JsonNode getInstanceJsonById(@PathVariable String instanceId);
}
