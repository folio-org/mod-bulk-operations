package org.folio.bulkops.client;

import org.folio.bulkops.domain.bean.ConfigurationCollection;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange(url = "users/configurations/entry", accept = MediaType.APPLICATION_JSON_VALUE)
public interface UserConfigurationClient {

  @GetExchange
  ConfigurationCollection getByQuery(
      @RequestParam("query") String query, @RequestParam("limit") Integer limit);
}
