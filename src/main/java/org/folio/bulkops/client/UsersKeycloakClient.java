package org.folio.bulkops.client;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.DeleteExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange(url = "users-keycloak", accept = MediaType.APPLICATION_JSON_VALUE)
public interface UsersKeycloakClient {

  @DeleteExchange(value = "/users/{userId}")
  void deleteUserById(@PathVariable String userId);
}
