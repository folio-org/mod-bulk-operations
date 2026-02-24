package org.folio.bulkops.client;

import org.folio.bulkops.domain.bean.NatureOfContentTerm;
import org.folio.bulkops.domain.bean.NatureOfContentTerms;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange(url = "nature-of-content-terms", accept = MediaType.APPLICATION_JSON_VALUE)
public interface NatureOfContentTermsClient {
  @GetExchange(value = "/{id}")
  NatureOfContentTerm getById(@PathVariable String id);

  @GetExchange
  NatureOfContentTerms getByQuery(@RequestParam String query, @RequestParam long limit);
}
