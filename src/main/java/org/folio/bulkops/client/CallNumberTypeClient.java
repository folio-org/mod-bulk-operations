package org.folio.bulkops.client;

import org.folio.bulkops.domain.bean.CallNumberType;
import org.folio.bulkops.domain.bean.CallNumberTypeCollection;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange(url = "call-number-types", accept = MediaType.APPLICATION_JSON_VALUE)
public interface CallNumberTypeClient {
  @GetExchange(value = "/{id}")
  CallNumberType getById(@PathVariable String id);

  @GetExchange
  CallNumberTypeCollection getByQuery(@RequestParam String query);
}
