package org.folio.bulkops.client;

import org.folio.bulkops.domain.bean.BriefInstanceCollection;
import org.folio.bulkops.domain.bean.InstanceCollection;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PatchExchange;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

@HttpExchange(url = "inventory/instances")
public interface InstanceClient {
  @PatchExchange(value = "/{instanceId}")
  void patchInstance(@RequestBody ObjectNode patch, @PathVariable String instanceId);

  @GetExchange(accept = MediaType.APPLICATION_JSON_VALUE)
  BriefInstanceCollection getByQuery(@RequestParam String query);

  @GetExchange(accept = MediaType.APPLICATION_JSON_VALUE)
  InstanceCollection getInstanceByQuery(
      @RequestParam("query") String query, @RequestParam long limit);

  @GetExchange(value = "/{instanceId}", accept = MediaType.APPLICATION_JSON_VALUE)
  JsonNode getInstanceJsonById(@PathVariable String instanceId);
}
