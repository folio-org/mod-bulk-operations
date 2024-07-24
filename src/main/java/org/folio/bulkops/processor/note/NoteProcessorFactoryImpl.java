package org.folio.bulkops.processor.note;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import static org.folio.bulkops.util.Constants.HOLDING_TYPE;
import static org.folio.bulkops.util.Constants.ITEM_TYPE;

@Component
@RequiredArgsConstructor
public class NoteProcessorFactoryImpl implements NoteProcessorFactory {

  private final ItemNoteProcessor itemNoteProcessor;
  private final HoldingsNotesProcessor holdingsNotesProcessor;

  @Override
  public AbstractNoteProcessor getNoteProcessor(String entityType) {
    return switch (entityType) {
      case ITEM_TYPE -> itemNoteProcessor;
      case HOLDING_TYPE -> holdingsNotesProcessor;
      default -> throw new IllegalStateException("Unexpected value: " + entityType);
    };
  }
}
