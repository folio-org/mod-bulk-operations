package org.folio.bulkops.client;

import org.folio.bulkops.domain.bean.ItemLocation;
import org.folio.bulkops.domain.bean.LoanType;
import org.folio.bulkops.domain.bean.LoanTypeCollection;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "loan-types")
public interface LoanTypeClient {
  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  LoanTypeCollection getByQuery(@RequestParam String query);

  @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
  LoanType getLoanTypeById(@PathVariable String id);
}
