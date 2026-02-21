package org.folio.bulkops.client;

import org.springframework.http.MediaType;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange(url = "mapping-rules", accept = MediaType.APPLICATION_JSON_VALUE)
public interface MappingRulesClient {

  @GetExchange(value = "/marc-bib")
  String getMarcBibMappingRules();
}
