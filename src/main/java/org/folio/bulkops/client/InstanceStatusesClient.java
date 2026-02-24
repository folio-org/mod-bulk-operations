package org.folio.bulkops.client;

import org.folio.bulkops.domain.bean.InstanceStatus;
import org.folio.bulkops.domain.bean.InstanceStatuses;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange(url = "instance-statuses", accept = MediaType.APPLICATION_JSON_VALUE)
public interface InstanceStatusesClient {
  @GetExchange(value = "/{id}")
  InstanceStatus getById(@PathVariable String id);

  @GetExchange
  InstanceStatuses getByQuery(@RequestParam String query, @RequestParam long limit);
}
