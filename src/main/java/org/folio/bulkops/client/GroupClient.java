package org.folio.bulkops.client;

import org.folio.bulkops.configs.FeignClientConfiguration;
import org.folio.bulkops.domain.bean.UserGroup;
import org.folio.bulkops.domain.bean.UserGroupCollection;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "groups", configuration = FeignClientConfiguration.class)
public interface GroupClient {

  @GetMapping(value = "/{groupId}", produces = MediaType.APPLICATION_JSON_VALUE)
  UserGroup getGroupById(@PathVariable String groupId);

  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  UserGroupCollection getByQuery(@RequestParam String query);
}
