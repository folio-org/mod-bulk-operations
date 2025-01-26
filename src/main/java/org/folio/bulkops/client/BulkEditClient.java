package org.folio.bulkops.client;

import java.util.UUID;

import org.folio.bulkops.configs.FeignClientConfiguration;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

@FeignClient(name = "bulk-edit", configuration = FeignClientConfiguration.class)
public interface BulkEditClient {
  @PostMapping(value = "/{jobId}/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  String uploadFile(@PathVariable UUID jobId, @RequestPart(value = "file") MultipartFile file);

  @PostMapping(value = "/{jobId}/start")
  void startJob(@PathVariable UUID jobId);
}
