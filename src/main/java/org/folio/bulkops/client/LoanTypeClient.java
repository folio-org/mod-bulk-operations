package org.folio.bulkops.client;

import org.folio.bulkops.domain.bean.LoanType;
import org.folio.bulkops.domain.bean.LoanTypeCollection;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange(url = "loan-types", accept = MediaType.APPLICATION_JSON_VALUE)
public interface LoanTypeClient {
  @GetExchange
  LoanTypeCollection getByQuery(@RequestParam String query);

  @GetExchange(value = "/{id}")
  LoanType getLoanTypeById(@PathVariable String id);
}
