package org.folio.bulkops.client;

import org.folio.bulkops.domain.dto.User;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "users")
public interface UserClient {

  @PutMapping(value = "/{userId}")
  void updateUser(@RequestBody User user, @PathVariable String userId);
}
