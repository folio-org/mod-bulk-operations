package org.folio.bulkops.client;

import org.folio.bulkops.configs.FeignClientConfiguration;
import org.folio.bulkops.domain.bean.MaterialType;
import org.folio.bulkops.domain.bean.MaterialTypeCollection;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "material-types", configuration = FeignClientConfiguration.class)
public interface MaterialTypeClient {

  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  MaterialTypeCollection getByQuery(@RequestParam String query);

  @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
  MaterialType getById(@PathVariable String id);
}
