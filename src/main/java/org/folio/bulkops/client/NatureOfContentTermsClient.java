package org.folio.bulkops.client;

import org.folio.bulkops.configs.FeignClientConfiguration;
import org.folio.bulkops.domain.bean.NatureOfContentTerm;
import org.folio.bulkops.domain.bean.NatureOfContentTerms;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "nature-of-content-terms", configuration = FeignClientConfiguration.class)
public interface NatureOfContentTermsClient {
  @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
  NatureOfContentTerm getById(@PathVariable String id);

  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  NatureOfContentTerms getByQuery(@RequestParam String query, @RequestParam long limit);
}
