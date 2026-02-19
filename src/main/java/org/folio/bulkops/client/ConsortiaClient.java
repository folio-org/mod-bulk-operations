package org.folio.bulkops.client;

import org.folio.bulkops.domain.bean.UserTenantCollection;
import org.springframework.http.MediaType;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange(url = "user-tenants", accept = MediaType.APPLICATION_JSON_VALUE)
public interface ConsortiaClient {

  @GetExchange(value = "?limit=1")
  UserTenantCollection getUserTenantCollection();
}
