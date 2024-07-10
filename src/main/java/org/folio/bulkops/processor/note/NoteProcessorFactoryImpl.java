package org.folio.bulkops.processor.note;

import lombok.RequiredArgsConstructor;
import org.folio.bulkops.service.HoldingsReferenceService;
import org.folio.bulkops.service.ItemReferenceService;
import org.folio.bulkops.service.NoteTableUpdater;
import org.springframework.stereotype.Component;

import static org.folio.bulkops.util.Constants.HOLDING_TYPE;
import static org.folio.bulkops.util.Constants.ITEM_TYPE;

@Component
@RequiredArgsConstructor
public class NoteProcessorFactoryImpl implements NoteProcessorFactory {

  private final HoldingsReferenceService holdingsReferenceService;
  private final ItemReferenceService itemReferenceService;
  private final NoteTableUpdater noteTableUpdater;

  @Override
  public AbstractNoteProcessor getNoteProcessor(String entityType) {
    return switch (entityType) {
      case ITEM_TYPE -> new ItemNoteProcessor(itemReferenceService, noteTableUpdater);
      case HOLDING_TYPE -> new HoldingsNotesProcessor(holdingsReferenceService, noteTableUpdater);
      default -> throw new IllegalStateException("Unexpected value: " + entityType);
    };
  }
}
