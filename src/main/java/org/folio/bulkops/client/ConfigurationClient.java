package org.folio.bulkops.client;

import org.folio.bulkops.configs.FeignClientConfiguration;
import org.folio.bulkops.domain.bean.ConfigurationCollection;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "configurations/entries", configuration = FeignClientConfiguration.class)
public interface ConfigurationClient {

  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  ConfigurationCollection getByQuery(@RequestParam("query") String query);
}
