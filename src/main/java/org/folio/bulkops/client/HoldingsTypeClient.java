package org.folio.bulkops.client;

import org.folio.bulkops.configs.FeignClientConfiguration;
import org.folio.bulkops.domain.dto.HoldingsType;
import org.folio.bulkops.domain.dto.HoldingsTypeCollection;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "holdings-types", configuration = FeignClientConfiguration.class)
public interface HoldingsTypeClient {
  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  HoldingsTypeCollection getByQuery(@RequestParam String query);

  @GetMapping(value = "/{holdingsTypeId}")
  HoldingsType getById(@PathVariable String holdingsTypeId);
}
