package org.folio.bulkops.client;

import org.folio.bulkops.domain.dto.ContributorTypeCollection;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange(url = "contributor-types", accept = MediaType.APPLICATION_JSON_VALUE)
public interface ContributorTypesClient {
  @GetExchange
  ContributorTypeCollection getByQuery(@RequestParam String query, @RequestParam long limit);
}
