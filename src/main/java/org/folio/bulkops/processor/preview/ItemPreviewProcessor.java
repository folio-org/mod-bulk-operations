package org.folio.bulkops.processor.preview;

import org.folio.bulkops.domain.bean.Item;
import org.springframework.stereotype.Component;

@Component
public class ItemPreviewProcessor extends AbstractPreviewProcessor<Item> {

  @Override
  public Class<Item> getProcessedType() {
    return Item.class;
  }
}
