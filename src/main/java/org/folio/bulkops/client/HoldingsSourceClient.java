package org.folio.bulkops.client;

import org.folio.bulkops.configs.FeignClientConfiguration;
import org.folio.bulkops.domain.dto.HoldingsRecordsSource;
import org.folio.bulkops.domain.dto.HoldingsRecordsSourceCollection;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "holdings-sources", configuration = FeignClientConfiguration.class)
public interface HoldingsSourceClient {
  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  HoldingsRecordsSourceCollection getByQuery(@RequestParam String query);

  @GetMapping(value = "/{holdingsSourceId}")
  HoldingsRecordsSource getById(@PathVariable String holdingsSourceId);
}
