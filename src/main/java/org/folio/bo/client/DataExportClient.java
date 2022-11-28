package org.folio.bo.client;

import org.folio.bo.domain.dto.Job;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

import static java.util.Objects.nonNull;

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
    var job = getJob(jobId);
    if (nonNull(job)) {
      var files = job.getFiles();
      // TODO
    }
    // TODO
    return null;
  }
}
