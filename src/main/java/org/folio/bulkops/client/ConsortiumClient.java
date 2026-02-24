package org.folio.bulkops.client;

import org.folio.bulkops.domain.bean.ConsortiaCollection;
import org.folio.bulkops.domain.bean.UserTenantCollection;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange(url = "consortia", accept = MediaType.APPLICATION_JSON_VALUE)
public interface ConsortiumClient {

  @GetExchange
  ConsortiaCollection getConsortia();

  @GetExchange(value = "/{consortiumId}/user-tenants")
  UserTenantCollection getConsortiaUserTenants(
      @PathVariable String consortiumId, @RequestParam String userId, @RequestParam int limit);
}
