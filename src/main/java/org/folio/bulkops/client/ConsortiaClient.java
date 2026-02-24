package org.folio.bulkops.client;

import org.folio.bulkops.domain.bean.UserTenantCollection;
import org.springframework.http.MediaType;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange(accept = MediaType.APPLICATION_JSON_VALUE)
public interface ConsortiaClient {

  @GetExchange(value = "user-tenants?limit=1")
  UserTenantCollection getUserTenantCollection();
}
