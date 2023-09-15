package org.folio.bulkops.service;

import joptsimple.internal.Strings;
import org.apache.commons.lang3.StringUtils;
import org.folio.bulkops.domain.dto.Cell;
import org.folio.bulkops.domain.dto.UnifiedTable;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;

import static org.folio.bulkops.util.Constants.ARRAY_DELIMITER;
import static org.folio.bulkops.util.Constants.ITEM_DELIMITER;
import static org.folio.bulkops.util.Constants.ITEM_DELIMITER_PATTERN;
import static org.folio.bulkops.util.Constants.ITEM_NOTE_POSITION;

@Component
public class ItemNoteTableUpdater {

  private static final String STAFF_ONLY = "(staff only)";

  public void extendTableWithItemNotesTypes(UnifiedTable unifiedTable) {
    int positionToInsert = ITEM_NOTE_POSITION + 1;
    var headers = unifiedTable.getHeader();
    var headerToRemove = headers.get(ITEM_NOTE_POSITION);
    var rows = unifiedTable.getRows();
    var notesPosition = new LinkedHashMap<String, Integer>();
    for (var row : rows) {
      var notesLine = row.getRow().get(ITEM_NOTE_POSITION);
      var notes = notesLine.split(ITEM_DELIMITER_PATTERN);
      for (var note : notes) {
        if (StringUtils.isEmpty(note)) continue;
        var notesFields = note.split(ARRAY_DELIMITER);
        var noteType = notesFields[0];
        if (!notesPosition.containsKey(noteType)) {
          headers.add(positionToInsert, new Cell().dataType(headerToRemove.getDataType())
            .visible(headerToRemove.getVisible()).value(noteType));
          notesPosition.put(noteType, positionToInsert++);
        }
      }
    }
    headers.remove(ITEM_NOTE_POSITION);
    for (var row : rows) {
      notesPosition.forEach((noteType, position) ->  row.getRow().add(position, Strings.EMPTY));
      var notesLine = row.getRow().get(ITEM_NOTE_POSITION);
      var notes = notesLine.split(ITEM_DELIMITER_PATTERN );
      for (var note : notes) {
        if (StringUtils.isEmpty(note)) continue;
        var notesFields = note.split(ARRAY_DELIMITER);
        var noteType = notesFields[0];
        var noteValue = notesFields[1];
        boolean noteStaffOnly = Boolean.parseBoolean(notesFields[2]);
        if (noteStaffOnly) noteValue += STAFF_ONLY;
        var existNoteValue = row.getRow().get(notesPosition.get(noteType));
        if (StringUtils.isNotEmpty(existNoteValue)) row.getRow().set(notesPosition.get(noteType), existNoteValue + ITEM_DELIMITER + noteValue);
        else row.getRow().set(notesPosition.get(noteType), noteValue);
      }
      row.getRow().remove(ITEM_NOTE_POSITION);
    }
  }
}
