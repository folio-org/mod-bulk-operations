package org.folio.bulkops.client;

import org.folio.bulkops.domain.bean.HoldingsRecord;
import org.folio.bulkops.domain.bean.HoldingsRecordCollection;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PatchExchange;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

@HttpExchange(url = "holdings-storage/holdings", accept = MediaType.APPLICATION_JSON_VALUE)
public interface HoldingsStorageClient {

  @GetExchange(value = "/{holdingsRecordId}")
  HoldingsRecord getHoldingById(@PathVariable String holdingsRecordId);

  @PatchExchange(value = "/{holdingsId}")
  void patchHoldingsRecord(@RequestBody ObjectNode patchRequest, @PathVariable String holdingsId);

  @GetExchange
  HoldingsRecordCollection getByQuery(@RequestParam String query, @RequestParam long limit);

  @GetExchange
  HoldingsRecordCollection getByQuery(@RequestParam String query);

  @GetExchange(value = "/{holdingsRecordId}")
  JsonNode getHoldingsJsonById(@PathVariable String holdingsRecordId);
}
