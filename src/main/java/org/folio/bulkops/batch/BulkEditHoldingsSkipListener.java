package org.folio.bulkops.batch;

import lombok.RequiredArgsConstructor;
import org.folio.bulkops.domain.bean.ExtendedHoldingsRecord;
import org.folio.bulkops.domain.bean.ItemIdentifier;
import org.folio.bulkops.exception.BulkEditException;
import org.springframework.batch.core.listener.SkipListener;
import org.springframework.stereotype.Component;

import java.util.List;

@RequiredArgsConstructor
@Component
public class BulkEditHoldingsSkipListener implements SkipListener<ItemIdentifier, List<ExtendedHoldingsRecord>> {

  private final BulkEditSkipListener bulkEditSkipListener;

  @Override
  public void onSkipInProcess(ItemIdentifier itemIdentifier, Throwable throwable) {
    if (throwable instanceof BulkEditException exception) {
      bulkEditSkipListener.onSkipInProcess(itemIdentifier, exception);
    }
  }
}
