package org.folio.bulkops.batch;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.folio.bulkops.domain.bean.ExtendedInstance;
import org.folio.bulkops.domain.bean.ItemIdentifier;
import org.folio.bulkops.exception.BulkEditException;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.listener.SkipListener;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
@StepScope
public class BulkEditInstanceSkipListener
    implements SkipListener<ItemIdentifier, List<ExtendedInstance>> {

  private final BulkEditSkipListener bulkEditSkipListener;

  @Override
  public void onSkipInProcess(ItemIdentifier itemIdentifier, Throwable throwable) {
    if (throwable instanceof BulkEditException exception) {
      bulkEditSkipListener.onSkipInProcess(itemIdentifier, exception);
    }
  }
}
