package org.folio.bulkops.client;

import org.folio.bulkops.configs.FeignClientConfiguration;
import org.folio.bulkops.domain.bean.SrsRecord;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "source-storage", configuration = FeignClientConfiguration.class)
public interface SrsClient {

  @GetMapping(value = "/source-records/{sourceRecordId}", produces = MediaType.APPLICATION_JSON_VALUE)
  SrsRecord getSrsRecordById(@PathVariable String sourceRecordId);
}
