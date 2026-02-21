package org.folio.bulkops.client;

import org.folio.bulkops.domain.bean.GetParsedRecordsBatchRequestBody;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;
import tools.jackson.databind.JsonNode;

@HttpExchange(url = "source-storage")
public interface SrsClient {

  @PostExchange(value = "/batch/parsed-records/fetch", accept = MediaType.APPLICATION_JSON_VALUE)
  JsonNode getParsedRecordsInBatch(@RequestBody GetParsedRecordsBatchRequestBody body);

  @GetExchange(value = "/source-records", accept = MediaType.APPLICATION_JSON_VALUE)
  JsonNode getMarc(
      @RequestParam("instanceId") String instanceId,
      @RequestParam("idType") String idType,
      @RequestParam("deleted") boolean deleted);
}
