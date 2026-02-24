package org.folio.bulkops.client;

import org.folio.bulkops.domain.bean.MaterialType;
import org.folio.bulkops.domain.bean.MaterialTypeCollection;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange(url = "material-types", accept = MediaType.APPLICATION_JSON_VALUE)
public interface MaterialTypeClient {

  @GetExchange
  MaterialTypeCollection getByQuery(@RequestParam String query);

  @GetExchange(value = "/{id}")
  MaterialType getById(@PathVariable String id);
}
