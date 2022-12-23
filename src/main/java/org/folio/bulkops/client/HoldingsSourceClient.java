package org.folio.bulkops.client;

import org.folio.bulkops.domain.bean.HoldingsRecordsSource;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "holdings-sources")
public interface HoldingsSourceClient {
  @GetMapping(value = "/{holdingsSourceId}")
  HoldingsRecordsSource getById(@PathVariable String holdingsSourceId);
}
