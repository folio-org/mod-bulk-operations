package org.folio.bulkops.client;

import java.util.UUID;

import org.folio.bulkops.configs.FeignClientConfiguration;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.multipart.MultipartFile;

@FeignClient(name = "bulk-edit", configuration = FeignClientConfiguration.class)
public interface BulkEditClient {
  @PostMapping(value = "/{jobId}/upload")
  String uploadFile(@PathVariable UUID jobId, @RequestBody MultipartFile file);

  @PostMapping(value = "/{jobId}/start")
  void startJob(@PathVariable UUID jobId);
}
