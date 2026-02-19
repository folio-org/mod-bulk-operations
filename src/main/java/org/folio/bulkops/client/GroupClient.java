package org.folio.bulkops.client;

import org.folio.bulkops.domain.bean.UserGroup;
import org.folio.bulkops.domain.bean.UserGroupCollection;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange(url = "groups", accept = MediaType.APPLICATION_JSON_VALUE)
public interface GroupClient {

  @GetExchange(value = "/{groupId}")
  UserGroup getGroupById(@PathVariable String groupId);

  @GetExchange
  UserGroupCollection getByQuery(@RequestParam String query);
}
