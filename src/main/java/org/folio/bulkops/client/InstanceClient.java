package org.folio.bulkops.client;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import org.folio.bulkops.configs.FeignClientConfiguration;
import org.folio.bulkops.domain.bean.BriefInstanceCollection;
import org.folio.bulkops.domain.bean.Instance;
import org.folio.bulkops.domain.bean.InstanceCollection;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "inventory/instances", configuration = FeignClientConfiguration .class)
public interface InstanceClient {
  @PutMapping(value = "/{instanceId}")
  void updateInstance(@RequestBody Instance instance, @PathVariable String instanceId);

  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  BriefInstanceCollection getByQuery(@RequestParam String query);

  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  InstanceCollection getInstanceByQuery(@RequestParam("query") String query, @RequestParam long limit);

  @GetMapping(value = "/{instanceId}", produces = MediaType.APPLICATION_JSON_VALUE)
  JsonNode getInstanceJsonById(@PathVariable String instanceId);
}
