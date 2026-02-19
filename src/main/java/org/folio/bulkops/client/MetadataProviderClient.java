package org.folio.bulkops.client;

import java.util.UUID;
import org.folio.bulkops.domain.bean.JobLogEntryCollection;
import org.folio.bulkops.domain.dto.DataImportJobExecutionCollection;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange(url = "metadata-provider", accept = MediaType.APPLICATION_JSON_VALUE)
public interface MetadataProviderClient {

  @GetExchange(value = "/jobExecutions")
  DataImportJobExecutionCollection getJobExecutionsByJobProfileId(
      @RequestParam("profileIdAny") UUID profileId, @RequestParam long limit);

  @GetExchange(value = "/jobLogEntries/{jobExecutionId}")
  JobLogEntryCollection getJobLogEntries(
      @PathVariable String jobExecutionId, @RequestParam long limit);

  @GetExchange(value = "/jobLogEntries/{jobExecutionId}")
  JobLogEntryCollection getJobLogEntries(
      @PathVariable String jobExecutionId, @RequestParam long offset, @RequestParam long limit);
}
