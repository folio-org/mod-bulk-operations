package org.folio.bulkops.client;

import org.folio.bulkops.domain.bean.HoldingsType;
import org.folio.bulkops.domain.bean.HoldingsTypeCollection;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange(url = "holdings-types", accept = MediaType.APPLICATION_JSON_VALUE)
public interface HoldingsTypeClient {
  @GetExchange
  HoldingsTypeCollection getByQuery(@RequestParam String query);

  @GetExchange(value = "/{holdingsTypeId}")
  HoldingsType getById(@PathVariable String holdingsTypeId);
}
