package org.folio.bulkops.client;

import org.folio.bulkops.domain.bean.HoldingsRecordsSource;
import org.folio.bulkops.domain.bean.HoldingsRecordsSourceCollection;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange(url = "holdings-sources")
public interface HoldingsSourceClient {
  @GetExchange(accept = MediaType.APPLICATION_JSON_VALUE)
  HoldingsRecordsSourceCollection getByQuery(@RequestParam String query);

  @GetExchange(value = "/{holdingsSourceId}")
  HoldingsRecordsSource getById(@PathVariable String holdingsSourceId);
}
