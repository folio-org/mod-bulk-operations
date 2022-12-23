package org.folio.bulkops.client;

import io.swagger.v3.oas.annotations.parameters.RequestBody;
import org.folio.bulkops.configs.FeignEncoderConfiguration;
import org.folio.bulkops.domain.dto.HoldingsRecord;
import org.folio.bulkops.domain.dto.HoldingsRecordCollection;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "holdings-storage/holdings")
public interface HoldingsClient {
  @PutMapping(value = "/{holdingsId}")
  void updateHoldingsRecord(@RequestBody HoldingsRecord holdingsRecord, @PathVariable String holdingsId);

  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  HoldingsRecordCollection getHoldingsByQuery(@RequestParam String query, @RequestParam long offset, @RequestParam long limit);

}
