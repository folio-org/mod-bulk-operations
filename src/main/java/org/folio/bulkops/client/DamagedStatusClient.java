package org.folio.bulkops.client;

import org.folio.bulkops.domain.bean.DamagedStatus;
import org.folio.bulkops.domain.bean.DamagedStatusCollection;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange(url = "item-damaged-statuses", accept = MediaType.APPLICATION_JSON_VALUE)
public interface DamagedStatusClient {
  @GetExchange(value = "/{id}")
  DamagedStatus getById(@PathVariable String id);

  @GetExchange
  DamagedStatusCollection getByQuery(@RequestParam String query);
}
