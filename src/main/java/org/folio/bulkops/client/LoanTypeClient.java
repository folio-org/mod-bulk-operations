package org.folio.bulkops.client;

import org.folio.bulkops.domain.bean.LoanType;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "loan-types")
public interface LoanTypeClient {
  @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
  LoanType getLoanTypeById(@PathVariable String id);
}
