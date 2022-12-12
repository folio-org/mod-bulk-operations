package org.folio.bulkops.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@FeignClient(name = "bulk-edit")
public interface BulkEditClient {
  @PostMapping(value = "/{jobId}/upload")
  String uploadFile(@PathVariable UUID jobId, @RequestBody MultipartFile file);
}
