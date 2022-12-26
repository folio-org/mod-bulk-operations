package org.folio.bulkops.client;

import org.folio.bulkops.configs.FeignClientConfiguration;
import org.folio.bulkops.domain.bean.ConfigurationCollection;
import org.folio.bulkops.domain.bean.ModelConfiguration;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "configurations/entries", configuration = FeignClientConfiguration.class)
public interface ConfigurationClient {

  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  ConfigurationCollection getConfigurations(@RequestParam("query") String query);

  @PostMapping
  ModelConfiguration postConfiguration(@RequestBody ModelConfiguration config);
}
