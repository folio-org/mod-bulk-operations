package org.folio.bulkops.client;

import com.fasterxml.jackson.databind.JsonNode;
import org.folio.bulkops.domain.bean.BriefInstanceCollection;
import org.folio.bulkops.domain.bean.InstanceCollection;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "inventory/instances")
public interface InstanceClient {
  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  BriefInstanceCollection getByQuery(@RequestParam String query);

  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  InstanceCollection getInstanceByQuery(@RequestParam("query") String query, @RequestParam long limit);

  @GetMapping(value = "/{instanceId}", produces = MediaType.APPLICATION_JSON_VALUE)
  JsonNode getInstanceJsonById(@PathVariable String instanceId);
}
