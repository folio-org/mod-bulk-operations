package org.folio.bulkops.client;

import org.folio.bulkops.domain.bean.UserPermissions;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange(url = "perms/users", accept = MediaType.APPLICATION_JSON_VALUE)
public interface OkapiUserPermissionsClient {

  @GetExchange(value = "/{userId}/permissions?expanded=true&indexField=userId")
  UserPermissions getPermissions(@PathVariable String userId);
}
