package org.folio.bulkops.processor.preview;

import org.folio.bulkops.domain.bean.Item;
import org.folio.bulkops.domain.dto.Row;
import org.folio.bulkops.processor.folio.ItemCirculationNoteProcessor;
import org.springframework.stereotype.Component;

@Component
public class ItemPreviewProcessor extends AbstractPreviewProcessor<Item> {
  @Override
  public Row transformToRow(Item item) {
    return super.transformToRow(ItemCirculationNoteProcessor.splitCirculationNotes(item));
  }

  @Override
  public Class<Item> getProcessedType() {
    return Item.class;
  }
}
