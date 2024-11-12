package org.folio.bulkops.client;

import org.folio.bulkops.configs.FeignClientConfiguration;
import org.folio.bulkops.domain.bean.JobLogEntryCollection;
import org.folio.bulkops.domain.bean.JournalRecordCollection;
import org.folio.bulkops.domain.dto.DataImportJobExecutionCollection;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

@FeignClient(name = "metadata-provider", configuration = FeignClientConfiguration.class)
public interface MetadataProviderClient {

  @GetMapping(value = "/jobExecutions", produces = MediaType.APPLICATION_JSON_VALUE)
  DataImportJobExecutionCollection getJobExecutionsByJobProfileId(@RequestParam("profileIdAny") UUID profileId,
    @RequestParam long limit);

  @GetMapping(value = "/jobLogEntries/{jobExecutionId}", produces = MediaType.APPLICATION_JSON_VALUE)
  JobLogEntryCollection getJobLogEntries(@PathVariable String jobExecutionId, @RequestParam long limit);

  @GetMapping(value = "/jobLogEntries/{jobExecutionId}", produces = MediaType.APPLICATION_JSON_VALUE)
  JobLogEntryCollection getJobLogEntries(@PathVariable String jobExecutionId, @RequestParam long offset, @RequestParam long limit);
}
