package org.folio.bulkops.client;

import org.folio.bulkops.configs.FeignClientConfiguration;
import org.folio.bulkops.domain.bean.CustomFieldCollection;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "custom-fields", configuration = FeignClientConfiguration.class)
public interface CustomFieldsClient {

  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  CustomFieldCollection getByQuery(@RequestHeader(value = "x-okapi-module-id") String moduleId,
                                   @RequestParam("query") String query);
}
