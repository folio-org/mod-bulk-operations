package org.folio.bulkops.client;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.util.UUID;

import org.folio.bulkops.domain.bean.ConsortiumHoldingCollection;
import org.folio.bulkops.domain.bean.ConsortiumItemCollection;
import org.folio.bulkops.domain.dto.BatchIdsDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "search/consortium")
public interface SearchConsortium {

  @GetMapping(value="/holdings", produces = APPLICATION_JSON_VALUE, consumes = APPLICATION_JSON_VALUE)
  ConsortiumHoldingCollection getHoldingsById(@RequestParam UUID instanceId);

  @PostMapping(value = "/batch/holdings", headers = {"Accept=application/json"})
  ConsortiumHoldingCollection getConsortiumHoldingCollection(@RequestBody BatchIdsDto batchIdsDto);

  @PostMapping(value = "/batch/items", headers = {"Accept=application/json"})
  ConsortiumItemCollection getConsortiumItemCollection(@RequestBody BatchIdsDto batchIdsDto);

}
