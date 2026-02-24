package org.folio.bulkops.client;

import org.folio.bulkops.domain.bean.SubjectSource;
import org.folio.bulkops.domain.bean.SubjectSourceCollection;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange(url = "subject-sources", accept = MediaType.APPLICATION_JSON_VALUE)
public interface SubjectSourcesClient {

  @GetExchange(value = "/{id}")
  SubjectSource getById(@PathVariable String id);

  @GetExchange
  SubjectSourceCollection getByQuery(@RequestParam String query);
}
