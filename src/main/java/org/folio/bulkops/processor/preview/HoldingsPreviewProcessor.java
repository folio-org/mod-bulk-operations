package org.folio.bulkops.processor.preview;

import org.folio.bulkops.domain.bean.HoldingsRecord;
import org.springframework.stereotype.Component;

@Component
public class HoldingsPreviewProcessor extends AbstractPreviewProcessor<HoldingsRecord> {

  @Override
  public Class<HoldingsRecord> getProcessedType() {
    return HoldingsRecord.class;
  }
}
