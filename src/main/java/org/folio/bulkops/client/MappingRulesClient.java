package org.folio.bulkops.client;

import org.folio.bulkops.configs.FeignClientConfiguration;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(name = "mapping-rules", configuration = FeignClientConfiguration.class)
public interface MappingRulesClient {

  @GetMapping(value = "/marc-bib", produces = MediaType.APPLICATION_JSON_VALUE)
  String getMarcBibMappingRules();
}
