package org.folio.bulkops.client;

import org.folio.bulkops.domain.bean.ClassificationType;
import org.folio.bulkops.domain.bean.ClassificationTypeCollection;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange(url = "classification-types", accept = MediaType.APPLICATION_JSON_VALUE)
public interface ClassificationTypesClient {

  @GetExchange(value = "/{id}")
  ClassificationType getById(@PathVariable String id);

  @GetExchange
  ClassificationTypeCollection getByQuery(@RequestParam String query);
}
