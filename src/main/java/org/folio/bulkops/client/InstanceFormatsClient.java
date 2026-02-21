package org.folio.bulkops.client;

import org.folio.bulkops.domain.bean.InstanceFormat;
import org.folio.bulkops.domain.bean.InstanceFormats;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange(url = "instance-formats", accept = MediaType.APPLICATION_JSON_VALUE)
public interface InstanceFormatsClient {
  @GetExchange(value = "/{id}")
  InstanceFormat getById(@PathVariable String id);

  @GetExchange
  InstanceFormats getByQuery(@RequestParam String query, @RequestParam long limit);
}
