package org.folio.bulkops.client;

import org.folio.bulkops.configs.FeignClientConfiguration;
import org.folio.bulkops.domain.bean.ClassificationType;
import org.folio.bulkops.domain.bean.ClassificationTypeCollection;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "classification-types", configuration = FeignClientConfiguration.class)
public interface ClassificationTypesClient {

  @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
  ClassificationType getById(@PathVariable String id);

  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  ClassificationTypeCollection getByQuery(@RequestParam String query);
}
