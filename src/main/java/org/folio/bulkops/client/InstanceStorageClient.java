package org.folio.bulkops.client;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import tools.jackson.databind.JsonNode;

@HttpExchange(url = "instance-storage/instances", accept = MediaType.APPLICATION_JSON_VALUE)
public interface InstanceStorageClient {

  @GetExchange(value = "/{instanceId}")
  JsonNode getInstanceJsonById(@PathVariable String instanceId);
}
