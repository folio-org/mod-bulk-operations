package org.folio.bulkops.service;

import static java.lang.Boolean.TRUE;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.folio.bulkops.util.Constants.ARRAY_DELIMITER;
import static org.folio.bulkops.util.Constants.ITEM_DELIMITER;
import static org.folio.bulkops.util.Constants.ITEM_DELIMITER_PATTERN;
import static org.folio.bulkops.util.Constants.ITEM_NOTE_POSITION;
import static org.folio.bulkops.util.Constants.STAFF_ONLY;

import lombok.RequiredArgsConstructor;
import org.folio.bulkops.domain.bean.NoteType;
import org.folio.bulkops.domain.dto.Cell;
import org.folio.bulkops.domain.dto.Row;
import org.folio.bulkops.domain.dto.UnifiedTable;

import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ItemNoteTableUpdater {
  private static final int NON_EXISTING_POSITION = -1;
  private static final int NUMBER_OF_NOTE_FIELDS = 3;
  private static final int NOTE_TYPE_POS = 0;
  private static final int NOTE_VALUE_POS = 1;
  private static final int STAFF_ONLY_FLAG_POS = 2;

  private final ItemReferenceService itemReferenceService;

  public void extendTableWithItemNotesTypes(UnifiedTable unifiedTable) {
    var noteTypeNames = itemReferenceService.getAllItemNoteTypes().stream()
      .map(NoteType::getName)
      .sorted()
      .toList();

    extendHeadersWithItemNoteTypeNames(unifiedTable.getHeader(), noteTypeNames);
    unifiedTable.getRows().forEach(row -> extendRowWithNotesData(row, noteTypeNames));
  }

  private void extendHeadersWithItemNoteTypeNames(List<Cell> headers, List<String> noteTypeNames) {
    var headerToReplace = headers.get(ITEM_NOTE_POSITION);
    var cellsToInsert = noteTypeNames.stream()
      .map(name -> new Cell()
        .value(name)
        .visible(headerToReplace.getVisible())
        .dataType(headerToReplace.getDataType())
        .ignoreTranslation(true))
      .toList();
    headers.remove(ITEM_NOTE_POSITION);
    headers.addAll(ITEM_NOTE_POSITION, cellsToInsert);
  }

  private void extendRowWithNotesData(Row row, List<String> noteTypeNames) {
    var notesArray = new String[noteTypeNames.size()];
    var rowList = row.getRow();
    var notesString = rowList.get(ITEM_NOTE_POSITION);
    if (isNotEmpty(notesString)) {
      for (var note : notesString.split(ITEM_DELIMITER_PATTERN)) {
        var noteFields = note.split(ARRAY_DELIMITER);
        if (noteFields.length == NUMBER_OF_NOTE_FIELDS) {
          var position = noteTypeNames.indexOf(noteFields[NOTE_TYPE_POS]);
          if (position != NON_EXISTING_POSITION) {
            var staffOnlyPostfix = TRUE.equals(Boolean.parseBoolean(noteFields[STAFF_ONLY_FLAG_POS])) ? STAFF_ONLY : EMPTY;
            var value = noteFields[NOTE_VALUE_POS] + staffOnlyPostfix;
            notesArray[position] = isEmpty(notesArray[position]) ? value : String.join(ITEM_DELIMITER, notesArray[position], value);
          }
        }
      }
    }
    rowList.remove(ITEM_NOTE_POSITION);
    rowList.addAll(ITEM_NOTE_POSITION, Arrays.asList(notesArray));
    row.setRow(rowList);
  }
}
