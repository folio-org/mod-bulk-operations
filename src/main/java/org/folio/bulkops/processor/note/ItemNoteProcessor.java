package org.folio.bulkops.processor.note;

import lombok.extern.log4j.Log4j2;
import org.folio.bulkops.domain.bean.NoteType;
import org.folio.bulkops.service.ItemReferenceService;
import org.folio.bulkops.service.NoteTableUpdater;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

import static org.folio.bulkops.util.Constants.ITEM_NOTE_POSITION;

@Component
@Log4j2
public class ItemNoteProcessor extends AbstractNoteProcessor {

  private final ItemReferenceService itemReferenceService;

  public ItemNoteProcessor(ItemReferenceService itemReferenceService, NoteTableUpdater noteTableUpdater) {
    super(noteTableUpdater);
    this.itemReferenceService = itemReferenceService;
  }

  @Override
  public List<String> getNoteTypeNames() {
    return itemReferenceService.getAllItemNoteTypes().stream()
      .map(NoteType::getName)
      .filter(Objects::nonNull)
      .sorted()
      .toList();
  }

  @Override
  public int getNotePosition() {
    return ITEM_NOTE_POSITION;
  }
}
