package org.folio.bulkops.client;

import io.swagger.v3.oas.annotations.parameters.RequestBody;
import org.folio.bulkops.config.FeignEncoderConfiguration;
import org.folio.bulkops.domain.dto.HoldingsRecord;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;

@FeignClient(name = "holdings-storage/holdings", configuration = FeignEncoderConfiguration.class)
public interface HoldingsClient {
  @PutMapping(value = "/{holdingsId}")
  void updateHoldingsRecord(@RequestBody HoldingsRecord holdingsRecord, @PathVariable String holdingsId);
}
