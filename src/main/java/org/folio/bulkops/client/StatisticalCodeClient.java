package org.folio.bulkops.client;

import org.folio.bulkops.domain.bean.StatisticalCode;
import org.folio.bulkops.domain.bean.StatisticalCodeCollection;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange(url = "statistical-codes", accept = MediaType.APPLICATION_JSON_VALUE)
public interface StatisticalCodeClient {
  @GetExchange(value = "/{id}")
  StatisticalCode getById(@PathVariable String id);

  @GetExchange
  StatisticalCodeCollection getByQuery(@RequestParam String query);
}
