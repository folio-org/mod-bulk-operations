package org.folio.bulkops.processor;

import org.folio.bulkops.client.HoldingsClient;
import org.folio.bulkops.domain.bean.HoldingsRecord;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class HoldingsUpdateProcessor implements UpdateProcessor<HoldingsRecord> {
  private final HoldingsClient holdingsClient;

  @Override
  public void updateRecord(HoldingsRecord holdingsRecord) {
    holdingsClient.updateHoldingsRecord(
      holdingsRecord.withInstanceHrid(null).withItemBarcode(null).withInstanceTitle(null),
      holdingsRecord.getId()
    );
  }

  @Override
  public Class<HoldingsRecord> getUpdatedType() {
    return HoldingsRecord.class;
  }
}
