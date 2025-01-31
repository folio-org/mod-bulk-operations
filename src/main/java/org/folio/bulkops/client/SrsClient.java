package org.folio.bulkops.client;

import com.fasterxml.jackson.databind.JsonNode;
import org.folio.bulkops.configs.FeignClientConfiguration;
import org.folio.bulkops.domain.bean.GetParsedRecordsBatchRequestBody;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "source-storage", configuration = FeignClientConfiguration.class)
public interface SrsClient {

  @PostMapping(value = "/batch/parsed-records/fetch", produces = MediaType.APPLICATION_JSON_VALUE)
  JsonNode getParsedRecordsInBatch(@RequestBody GetParsedRecordsBatchRequestBody body);
}
