package org.folio.bulkops.service;

import org.apache.commons.lang3.StringUtils;
import org.folio.bulkops.domain.bean.Item;
import org.folio.bulkops.domain.dto.Row;
import org.folio.bulkops.util.UnifiedTableHeaderBuilder;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.stream.IntStream;

import static org.folio.bulkops.util.Constants.ITEM_NOTE_POSITION;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class ItemNoteTableUpdaterTest {

  @Test
  void extendTableWithItemNotesTypesTest() {
    int administrativeNotesPosition = 30;
    int circulationNotesPosition = 32;
    int circulationNotesPositionAfterShifting = 33;

    var itemNoteTableUpdater = new ItemNoteTableUpdater();
    var itemLastFieldPosition = 47;
    var itemFieldsSize = itemLastFieldPosition + 1;
    var table = UnifiedTableHeaderBuilder.getEmptyTableWithHeaders(Item.class);
    var row = new ArrayList<String>();
    IntStream.range(0, itemFieldsSize).forEach(i -> row.add(StringUtils.EMPTY));
    row.set(ITEM_NOTE_POSITION, "NoteType;value1;false|NoteType;value2;true");
    row.set(administrativeNotesPosition, "administrative notes");
    row.set(circulationNotesPosition, "circulation notes");
    var rowItem = new Row();
    rowItem.setRow(row);

    var row2 = new ArrayList<String>();
    IntStream.range(0, itemFieldsSize).forEach(i -> row2.add(StringUtils.EMPTY));
    row2.set(ITEM_NOTE_POSITION, "NoteType2;value3;false");
    row2.set(administrativeNotesPosition, "administrative notes");
    row2.set(circulationNotesPosition, "circulation notes");
    var rowItem2 = new Row();
    rowItem2.setRow(row2);

    table.addRowsItem(rowItem);
    table.addRowsItem(rowItem2);

    itemNoteTableUpdater.extendTableWithItemNotesTypes(table);

    var headers = table.getHeader();
    int expectedSize = itemFieldsSize + 1;
    assertEquals(expectedSize, headers.size());

    var headerCell = headers.get(ITEM_NOTE_POSITION);
    assertEquals("NoteType", headerCell.getValue());

    headerCell = headers.get(ITEM_NOTE_POSITION + 1);
    assertEquals("NoteType2", headerCell.getValue());

    var actualRow1 = table.getRows().get(0);
    var actualRow2 = table.getRows().get(1);
    assertEquals(expectedSize, actualRow1.getRow().size());
    assertEquals(expectedSize, actualRow2.getRow().size());

    assertEquals("administrative notes", actualRow1.getRow().get(administrativeNotesPosition));
    assertEquals("value1|value2(staff only)", actualRow1.getRow().get(ITEM_NOTE_POSITION));
    assertEquals(StringUtils.EMPTY, actualRow1.getRow().get(ITEM_NOTE_POSITION + 1));
    assertEquals("circulation notes", actualRow1.getRow().get(circulationNotesPositionAfterShifting));

    assertEquals("administrative notes", actualRow1.getRow().get(administrativeNotesPosition));
    assertEquals(StringUtils.EMPTY, actualRow2.getRow().get(ITEM_NOTE_POSITION));
    assertEquals("value3", actualRow2.getRow().get(ITEM_NOTE_POSITION + 1));
    assertEquals("circulation notes", actualRow1.getRow().get(circulationNotesPositionAfterShifting));
  }

  @Test
  void extendTableWithItemNotesTypesIfItemNotesEmptyTest() {
    int administrativeNotesPosition = 30;
    int circulationNotesPosition = 32;
    int circulationNotesPositionAfterShifting = 31;

    var itemNoteTableUpdater = new ItemNoteTableUpdater();
    var itemLastFieldPosition = 47;
    var itemFieldsSize = itemLastFieldPosition + 1;
    var table = UnifiedTableHeaderBuilder.getEmptyTableWithHeaders(Item.class);
    var row = new ArrayList<String>();
    IntStream.range(0, itemFieldsSize).forEach(i -> row.add(StringUtils.EMPTY));
    row.set(ITEM_NOTE_POSITION, "");
    row.set(administrativeNotesPosition, "administrative notes");
    row.set(circulationNotesPosition, "circulation notes");
    var rowItem = new Row();
    rowItem.setRow(row);
    table.addRowsItem(rowItem);

    itemNoteTableUpdater.extendTableWithItemNotesTypes(table);

    var headers = table.getHeader();
    int expectedSize = itemFieldsSize - 1;
    assertEquals(expectedSize, headers.size());

    var actualRow = table.getRows().get(0);

    assertEquals(expectedSize, actualRow.getRow().size());

    assertEquals("administrative notes", actualRow.getRow().get(administrativeNotesPosition));
    assertEquals("circulation notes", actualRow.getRow().get(circulationNotesPositionAfterShifting));
  }
}
