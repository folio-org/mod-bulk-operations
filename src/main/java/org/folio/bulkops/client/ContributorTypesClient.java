package org.folio.bulkops.client;

import org.folio.bulkops.configs.FeignClientConfiguration;
import org.folio.bulkops.domain.dto.ContributorTypeCollection;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "contributor-types", configuration = FeignClientConfiguration.class)
public interface ContributorTypesClient {
  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  ContributorTypeCollection getByQuery(@RequestParam String query, @RequestParam long limit);
}
