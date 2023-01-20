package org.folio.bulkops.client;

import org.folio.bulkops.configs.FeignClientConfiguration;
import org.folio.bulkops.domain.dto.Errors;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

@FeignClient(name = "data-export-worker", configuration = FeignClientConfiguration.class)
public interface DataExportWorkerClient {
  @GetMapping(value = "/{jobId}/errors")
  Errors getErrorsPreview(@PathVariable UUID jobId, @RequestParam int limit);
}
