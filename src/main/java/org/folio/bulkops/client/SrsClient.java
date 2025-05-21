package org.folio.bulkops.client;

import com.fasterxml.jackson.databind.JsonNode;
import org.folio.bulkops.configs.FeignClientConfiguration;
import org.folio.bulkops.domain.bean.GetParsedRecordsBatchRequestBody;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "source-storage", configuration = FeignClientConfiguration.class)
public interface SrsClient {

  @PostMapping(value = "/batch/parsed-records/fetch", produces = MediaType.APPLICATION_JSON_VALUE)
  JsonNode getParsedRecordsInBatch(@RequestBody GetParsedRecordsBatchRequestBody body);

  @GetMapping(value = "/source-records", produces = MediaType.APPLICATION_JSON_VALUE)
  JsonNode getMarc(@RequestParam("instanceId") String instanceId, @RequestParam("idType") String idType, @RequestParam("deleted") boolean deleted);
}
