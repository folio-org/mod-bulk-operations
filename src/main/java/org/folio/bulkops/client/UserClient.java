package org.folio.bulkops.client;

import io.swagger.v3.oas.annotations.parameters.RequestBody;
import org.folio.bulkops.configs.FeignClientConfiguration;
import org.folio.bulkops.domain.bean.User;
import org.folio.bulkops.domain.bean.UserCollection;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "users", configuration = FeignClientConfiguration.class)
public interface UserClient {

  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  UserCollection getByQuery(@RequestParam("query") String query, @RequestParam long limit);

  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  UserCollection getByQuery(
      @RequestParam("query") String query, @RequestParam long offset, @RequestParam long limit);

  @GetMapping(value = "/{userId}", produces = MediaType.APPLICATION_JSON_VALUE)
  User getUserById(@PathVariable String userId);

  @PutMapping(value = "/{userId}", consumes = MediaType.APPLICATION_JSON_VALUE)
  void updateUser(@RequestBody User user, @PathVariable String userId);
}
