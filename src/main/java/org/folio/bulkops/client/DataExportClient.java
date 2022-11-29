package org.folio.bulkops.client;

import org.folio.bulkops.domain.pojo.Job;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@FeignClient
public interface DataExportClient {

  @PostMapping(value = "data-export-spring/jobs")
  Job upsertJob(@RequestBody Job job);

  @GetMapping(value = "data-export-spring/jobs/{jobId}")
  Job getJob(@PathVariable UUID jobId);

  @PostMapping(value = "bulk-edit/{jobId}/upload")
  String uploadFile(@PathVariable UUID jobId, @RequestBody MultipartFile file);

  @PostMapping(value = "bulk-edit/{jobId}/start")
  String startJob(@PathVariable UUID jobId);

  default String deleteFile(UUID jobId) {
    return null;
  }
}
