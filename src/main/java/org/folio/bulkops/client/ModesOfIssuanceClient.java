package org.folio.bulkops.client;

import org.folio.bulkops.configs.FeignClientConfiguration;
import org.folio.bulkops.domain.bean.ModeOfIssuance;
import org.folio.bulkops.domain.bean.ModesOfIssuance;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "modes-of-issuance", configuration = FeignClientConfiguration.class)
public interface ModesOfIssuanceClient {
  @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
  ModeOfIssuance getById(@PathVariable String id);

  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  ModesOfIssuance getByQuery(@RequestParam String query, @RequestParam long limit);

}
