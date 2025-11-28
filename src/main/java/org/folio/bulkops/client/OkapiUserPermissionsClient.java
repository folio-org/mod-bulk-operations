package org.folio.bulkops.client;

import org.folio.bulkops.configs.FeignClientConfiguration;
import org.folio.bulkops.domain.bean.UserPermissions;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "perms/users", configuration = FeignClientConfiguration.class)
public interface OkapiUserPermissionsClient {

  @GetMapping(
      value = "/{userId}/permissions?expanded=true&indexField=userId",
      produces = MediaType.APPLICATION_JSON_VALUE)
  UserPermissions getPermissions(@PathVariable String userId);
}
