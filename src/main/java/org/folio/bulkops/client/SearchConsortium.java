package org.folio.bulkops.client;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.util.UUID;
import org.folio.bulkops.domain.bean.ConsortiumHoldingCollection;
import org.folio.bulkops.domain.bean.ConsortiumItemCollection;
import org.folio.bulkops.domain.dto.BatchIdsDto;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

@HttpExchange(url = "search/consortium")
public interface SearchConsortium {

  @GetExchange(value = "/holdings", accept = APPLICATION_JSON_VALUE)
  ConsortiumHoldingCollection getHoldingsById(@RequestParam UUID instanceId);

  @PostExchange(
      value = "/batch/holdings",
      headers = {"Accept=application/json"})
  ConsortiumHoldingCollection getConsortiumHoldingCollection(@RequestBody BatchIdsDto batchIdsDto);

  @PostExchange(
      value = "/batch/items",
      headers = {"Accept=application/json"})
  ConsortiumItemCollection getConsortiumItemCollection(@RequestBody BatchIdsDto batchIdsDto);
}
