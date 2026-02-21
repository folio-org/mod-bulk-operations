package org.folio.bulkops.client;

import java.util.List;
import org.folio.bulkops.domain.bean.UserPermissions;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange(url = "users-keycloak/users", accept = MediaType.APPLICATION_JSON_VALUE)
public interface EurekaUserPermissionsClient {

  @GetExchange(value = "/{userId}/permissions")
  UserPermissions getPermissions(
      @PathVariable String userId, @RequestParam List<String> desiredPermissions);
}
