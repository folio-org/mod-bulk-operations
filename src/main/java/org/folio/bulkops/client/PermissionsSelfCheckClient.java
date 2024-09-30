package org.folio.bulkops.client;

import java.util.List;

import org.folio.bulkops.configs.FeignClientConfiguration;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(name = "bulk-operations", configuration = FeignClientConfiguration.class)
public interface PermissionsSelfCheckClient {

  @GetMapping(value = "/permissions-self-check", produces = MediaType.APPLICATION_JSON_VALUE)
  List<String> getUserPermissionsForSelfCheck();
}
