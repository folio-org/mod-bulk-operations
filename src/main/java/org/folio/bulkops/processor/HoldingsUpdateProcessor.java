package org.folio.bulkops.processor;

import org.folio.bulkops.client.HoldingsClient;
import org.folio.bulkops.domain.bean.HoldingsRecord;
import org.folio.bulkops.service.RuleService;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class HoldingsUpdateProcessor implements UpdateProcessor<HoldingsRecord> {
  private final HoldingsClient holdingsClient;
  private final RuleService ruleService;

  @Override
  public void updateRecord(HoldingsRecord holdingsRecord, String identifier, UUID operationId) {
    holdingsClient.updateHoldingsRecord(holdingsRecord.withInstanceHrid(null)
        .withItemBarcode(null)
        .withInstanceTitle(null), holdingsRecord.getId());
  }

  @Override
  public Class<HoldingsRecord> getUpdatedType() {
    return HoldingsRecord.class;
  }
}
