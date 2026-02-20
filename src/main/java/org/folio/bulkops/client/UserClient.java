package org.folio.bulkops.client;

import org.folio.bulkops.domain.bean.User;
import org.folio.bulkops.domain.bean.UserCollection;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PutExchange;

@HttpExchange(url = "users", accept = MediaType.APPLICATION_JSON_VALUE)
public interface UserClient {

  @GetExchange
  UserCollection getByQuery(@RequestParam("query") String query, @RequestParam long limit);

  @GetExchange
  UserCollection getByQuery(
      @RequestParam("query") String query, @RequestParam long offset, @RequestParam long limit);

  @GetExchange(value = "/{userId}")
  User getUserById(@PathVariable String userId);

  @PutExchange(value = "/{userId}")
  void updateUser(@RequestBody User user, @PathVariable String userId);
}
