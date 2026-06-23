package org.folio.bulkops.client;

import org.folio.bulkops.domain.dto.OpenTransactions;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange(url = "bl-users", accept = MediaType.APPLICATION_JSON_VALUE)
public interface BlUsersClient {

  @GetExchange(value = "/by-id/{userId}/open-transactions")
  OpenTransactions getOpenTransactions(@PathVariable String userId);
}
