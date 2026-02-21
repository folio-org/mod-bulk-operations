package org.folio.bulkops.client;

import org.folio.bulkops.domain.bean.ModeOfIssuance;
import org.folio.bulkops.domain.bean.ModesOfIssuance;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange(url = "modes-of-issuance", accept = MediaType.APPLICATION_JSON_VALUE)
public interface ModesOfIssuanceClient {
  @GetExchange(value = "/{id}")
  ModeOfIssuance getById(@PathVariable String id);

  @GetExchange
  ModesOfIssuance getByQuery(@RequestParam String query, @RequestParam long limit);
}
