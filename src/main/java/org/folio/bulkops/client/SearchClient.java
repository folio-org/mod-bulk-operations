package org.folio.bulkops.client;

import org.folio.bulkops.configs.FeignClientConfiguration;
import org.folio.bulkops.domain.dto.BatchIdsDto;
import org.folio.bulkops.domain.dto.ConsortiumHoldingCollection;
import org.folio.bulkops.domain.dto.ConsortiumItemCollection;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
    name = "search",
    configuration = {FeignClientConfiguration.class})
public interface SearchClient {
  @PostMapping(
      value = "/consortium/batch/items",
      headers = {"Accept=application/json"})
  ConsortiumItemCollection getConsortiumItemCollection(@RequestBody BatchIdsDto batchIdsDto);

  @PostMapping(
      value = "/consortium/batch/holdings",
      headers = {"Accept=application/json"})
  ConsortiumHoldingCollection getConsortiumHoldingCollection(@RequestBody BatchIdsDto batchIdsDto);
}
