package org.folio.bulkops.client;

import java.util.List;

import org.folio.bulkops.configs.FeignClientConfiguration;
import org.folio.bulkops.domain.bean.UserPermissions;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "users-keycloak/users", configuration = FeignClientConfiguration.class)
public interface EurekaUserPermissionsClient {

  @GetMapping(value = "/{userId}/permissions", produces = MediaType.APPLICATION_JSON_VALUE)
  UserPermissions getPermissions(@PathVariable String userId, @RequestParam List<String> desiredPermissions);
}
