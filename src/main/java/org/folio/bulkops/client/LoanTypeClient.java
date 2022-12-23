package org.folio.bulkops.client;

import org.folio.bulkops.domain.dto.LoanTypeCollection;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "loan-types")
public interface LoanTypeClient {
  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  LoanTypeCollection getByQuery(@RequestParam String query);
}
