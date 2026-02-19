package org.folio.bulkops.client;

import io.swagger.v3.oas.annotations.parameters.RequestBody;
import org.folio.bulkops.domain.bean.HoldingsRecord;
import org.folio.bulkops.domain.bean.HoldingsRecordCollection;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PutExchange;
import tools.jackson.databind.JsonNode;

@HttpExchange(url = "holdings-storage/holdings", accept = MediaType.APPLICATION_JSON_VALUE)
public interface HoldingsStorageClient {

  @GetExchange(value = "/{holdingsRecordId}")
  HoldingsRecord getHoldingById(@PathVariable String holdingsRecordId);

  @PutExchange(value = "/{holdingsId}")
  void updateHoldingsRecord(
      @RequestBody HoldingsRecord holdingsRecord, @PathVariable String holdingsId);

  @GetExchange
  HoldingsRecordCollection getByQuery(@RequestParam String query, @RequestParam long limit);

  @GetExchange
  HoldingsRecordCollection getByQuery(@RequestParam String query);

  @GetExchange(value = "/{holdingsRecordId}")
  JsonNode getHoldingsJsonById(@PathVariable String holdingsRecordId);
}
