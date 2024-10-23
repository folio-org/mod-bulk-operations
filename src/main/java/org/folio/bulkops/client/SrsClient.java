package org.folio.bulkops.client;

import com.fasterxml.jackson.databind.JsonNode;
import org.folio.bulkops.configs.FeignClientConfiguration;
import org.folio.bulkops.domain.bean.GetParsedRecordsBatchRequestBody;
import org.folio.bulkops.domain.bean.SrsRecord;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "source-storage", configuration = FeignClientConfiguration.class)
public interface SrsClient {

  @GetMapping(value = "/source-records/{sourceRecordId}", produces = MediaType.APPLICATION_JSON_VALUE)
  SrsRecord getSrsRecordById(@PathVariable String sourceRecordId);

  @GetMapping(value = "/batch/parsed-records/fetch", produces = MediaType.APPLICATION_JSON_VALUE)
  JsonNode getParsedRecordsInBatch(@RequestBody GetParsedRecordsBatchRequestBody body);
}
