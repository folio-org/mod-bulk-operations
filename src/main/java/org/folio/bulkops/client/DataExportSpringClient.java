package org.folio.bulkops.client;

import org.folio.bulkops.domain.dto.Job;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.UUID;

import static java.util.Objects.nonNull;

@FeignClient(name = "data-export-spring")
public interface DataExportSpringClient {

  @PostMapping(value = "/jobs")
  Job upsertJob(@RequestBody Job job);

  @GetMapping(value = "/jobs/{jobId}")
  Job getJob(@PathVariable UUID jobId);

  // TODO //NOSONAR
  default String deleteFile(UUID jobId) {
    var job = getJob(jobId);
    if (nonNull(job)) {
      var files = job.getFiles(); //NOSONAR
    }
    return null; //NOSONAR
  }
}
