package org.folio.bulkops.client;

import org.folio.bulkops.configs.FeignClientConfiguration;
import org.folio.bulkops.domain.bean.IllPolicy;
import org.folio.bulkops.domain.bean.IllPolicyCollection;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "ill-policies", configuration = FeignClientConfiguration.class)
public interface IllPolicyClient {
  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  IllPolicyCollection getByQuery(@RequestParam String query);

  @GetMapping(value = "/{illPolicyId}")
  IllPolicy getById(@PathVariable String illPolicyId);
}
