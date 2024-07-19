package org.folio.bulkops.client;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.util.UUID;

import org.folio.bulkops.domain.bean.ConsortiumHoldingCollection;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "search/consortium/holdings")
public interface SearchConsortiumHoldings {

  @GetMapping(produces = APPLICATION_JSON_VALUE, consumes = APPLICATION_JSON_VALUE)
  ConsortiumHoldingCollection getHoldingsById(@RequestParam UUID instanceId);
}
