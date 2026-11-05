package org.folio.bulkops.client;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;
import tools.jackson.databind.JsonNode;

@HttpExchange(accept = MediaType.APPLICATION_JSON_VALUE)
public interface EntitiesLinksClient {

  @PostExchange(value = "links-suggestions/marc", contentType = MediaType.APPLICATION_JSON_VALUE)
  JsonNode suggestLinks(
      @RequestBody JsonNode parsedRecordContentCollection,
      @RequestParam("authoritySearchParameter") String authoritySearchParameter,
      @RequestParam("ignoreAutoLinkingEnabled") boolean ignoreAutoLinkingEnabled);

  @GetExchange(value = "linking-rules/instance-authority")
  JsonNode getLinkingRules();
}
