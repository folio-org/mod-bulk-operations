package org.folio.bulkops.client;

import org.folio.bulkops.domain.bean.SubjectType;
import org.folio.bulkops.domain.bean.SubjectTypeCollection;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange(url = "subject-types", accept = MediaType.APPLICATION_JSON_VALUE)
public interface SubjectTypesClient {

  @GetExchange(value = "/{id}")
  SubjectType getById(@PathVariable String id);

  @GetExchange
  SubjectTypeCollection getByQuery(@RequestParam String query);
}
