package org.folio.bulkops.client;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import org.folio.bulkops.configs.FeignEncoderConfiguration;
import org.folio.bulkops.domain.bean.HoldingsRecord;
import org.folio.bulkops.domain.bean.HoldingsRecordCollection;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "holdings-storage/holdings", configuration = FeignEncoderConfiguration.class)
public interface HoldingsClient {

  @GetMapping(value = "/{holdingsRecordId}", produces = MediaType.APPLICATION_JSON_VALUE)
  HoldingsRecord getHoldingById(@PathVariable String holdingsRecordId);

  @PutMapping(value = "/{holdingsId}")
  void updateHoldingsRecord(@RequestBody HoldingsRecord holdingsRecord, @PathVariable String holdingsId);

  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  HoldingsRecordCollection getByQuery(@RequestParam String query, @RequestParam long limit);

  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  HoldingsRecordCollection getByQuery(@RequestParam String query);

  @GetMapping(value = "/{holdingsRecordId}", produces = MediaType.APPLICATION_JSON_VALUE)
  JsonNode getHoldingsJsonById(@PathVariable String holdingsRecordId);
}
