package org.folio.bulkops.processor;

import lombok.RequiredArgsConstructor;
import org.folio.bulkops.client.HoldingsClient;
import org.springframework.stereotype.Component;
import org.folio.bulkops.domain.dto.HoldingsRecord;

@Component
@RequiredArgsConstructor
public class HoldingsUpdateProcessor implements UpdateProcessor<HoldingsRecord> {
  private final HoldingsClient holdingsClient;
  @Override
  public void updateRecord(HoldingsRecord holdingsRecord) {
    holdingsClient.updateHoldingsRecord(holdingsRecord, holdingsRecord.getId());
  }
}
