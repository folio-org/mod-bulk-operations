package org.folio.bulkops.client;

import org.folio.bulkops.configs.FeignClientConfiguration;
import org.folio.bulkops.domain.bean.SubjectType;
import org.folio.bulkops.domain.bean.SubjectTypeCollection;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "subject-types", configuration = FeignClientConfiguration.class)
public interface SubjectTypesClient {

  @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
  SubjectType getById(@PathVariable String id);

  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  SubjectTypeCollection getByQuery(@RequestParam String query);
}
