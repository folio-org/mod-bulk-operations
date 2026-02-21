package org.folio.bulkops.client;

import org.folio.bulkops.domain.dto.BatchIdsDto;
import org.folio.bulkops.domain.dto.ConsortiumHoldingCollection;
import org.folio.bulkops.domain.dto.ConsortiumItemCollection;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

@HttpExchange(url = "search")
public interface SearchClient {
  @PostExchange(
      value = "/consortium/batch/items",
      headers = {"Accept=application/json"})
  ConsortiumItemCollection getConsortiumItemCollection(@RequestBody BatchIdsDto batchIdsDto);

  @PostExchange(
      value = "/consortium/batch/holdings",
      headers = {"Accept=application/json"})
  ConsortiumHoldingCollection getConsortiumHoldingCollection(@RequestBody BatchIdsDto batchIdsDto);
}
