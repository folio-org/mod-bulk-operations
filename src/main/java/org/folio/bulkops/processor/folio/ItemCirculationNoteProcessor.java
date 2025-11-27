package org.folio.bulkops.processor.folio;

import static org.folio.bulkops.domain.bean.CirculationNote.NoteTypeEnum.IN;
import static org.folio.bulkops.domain.bean.CirculationNote.NoteTypeEnum.OUT;

import lombok.experimental.UtilityClass;
import org.folio.bulkops.domain.bean.Item;
import org.springframework.util.ObjectUtils;

@UtilityClass
public class ItemCirculationNoteProcessor {
  public static Item splitCirculationNotes(Item item) {
    var notes = item.getCirculationNotes();
    return ObjectUtils.isEmpty(notes)
        ? item
        : item.withCheckInNotes(
                notes.stream()
                    .filter(circulationNote -> IN.equals(circulationNote.getNoteType()))
                    .toList())
            .withCheckOutNotes(
                notes.stream()
                    .filter(circulationNote -> OUT.equals(circulationNote.getNoteType()))
                    .toList());
  }
}
