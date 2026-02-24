package org.folio.bulkops.client;

import org.folio.bulkops.domain.bean.StatisticalCodeType;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange(url = "statistical-code-types", accept = MediaType.APPLICATION_JSON_VALUE)
public interface StatisticalCodeTypeClient {

  @GetExchange(value = "/{id}")
  StatisticalCodeType getById(@PathVariable String id);
}
