package org.folio.bulkops.client;

import org.folio.bulkops.domain.bean.InstanceType;
import org.folio.bulkops.domain.bean.InstanceTypes;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange(url = "instance-types", accept = MediaType.APPLICATION_JSON_VALUE)
public interface InstanceTypesClient {
  @GetExchange(value = "/{id}")
  InstanceType getById(@PathVariable String id);

  @GetExchange
  InstanceTypes getByQuery(@RequestParam String query, @RequestParam long limit);
}
