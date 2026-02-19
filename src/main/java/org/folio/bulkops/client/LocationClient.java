package org.folio.bulkops.client;

import org.folio.bulkops.domain.bean.ItemLocation;
import org.folio.bulkops.domain.bean.ItemLocationCollection;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import tools.jackson.databind.JsonNode;

@HttpExchange(url = "locations", accept = MediaType.APPLICATION_JSON_VALUE)
public interface LocationClient {

  @GetExchange
  ItemLocationCollection getByQuery(@RequestParam String query);

  @GetExchange(value = "/{id}")
  ItemLocation getLocationById(@PathVariable String id);

  @GetExchange(value = "/{id}")
  JsonNode getLocationJsonById(@PathVariable String id);
}
