package org.folio.bulkops.client;

import org.folio.bulkops.configs.FeignClientConfiguration;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "source-storage/records", configuration = FeignClientConfiguration.class)
public interface SRSRecordsClient {
  @PutMapping(value = "/{id}/suppress-from-discovery", produces = MediaType.TEXT_PLAIN_VALUE)
  String setSuppressFromDiscovery(@PathVariable String id, @RequestParam("idType") String idType, @RequestParam("suppress") boolean suppress);
}
