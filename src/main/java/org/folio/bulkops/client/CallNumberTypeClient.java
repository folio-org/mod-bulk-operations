package org.folio.bulkops.client;

import org.folio.bulkops.configs.FeignClientConfiguration;
import org.folio.bulkops.domain.bean.CallNumberType;
import org.folio.bulkops.domain.bean.CallNumberTypeCollection;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "call-number-types", configuration = FeignClientConfiguration.class)
public interface CallNumberTypeClient {
  @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
  CallNumberType getById(@PathVariable String id);

  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  CallNumberTypeCollection getByQuery(@RequestParam String query);
}
