package org.folio.bulkops.service;

import static org.folio.bulkops.util.Constants.HOLDINGS_NOTE_POSITION;
import static org.folio.bulkops.util.Constants.ITEM_NOTE_POSITION;
import static org.folio.bulkops.util.Constants.STAFF_ONLY;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import org.folio.bulkops.domain.bean.HoldingsNoteType;
import org.folio.bulkops.domain.bean.Item;
import org.folio.bulkops.domain.bean.NoteType;
import org.folio.bulkops.domain.dto.Cell;
import org.folio.bulkops.domain.dto.Row;
import org.folio.bulkops.util.UnifiedTableHeaderBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@ExtendWith(MockitoExtension.class)
class ItemNoteTableUpdaterTest {
  private static final int ACTION_NOTE_POSITION = ITEM_NOTE_POSITION;
  private static final int BINDING_POSITION = ITEM_NOTE_POSITION + 1;
  private static final int NOTE_POSITION = ITEM_NOTE_POSITION + 2;

  private static final int ACTION_HOLDINGS_NOTE_POSITION = HOLDINGS_NOTE_POSITION;
  private static final int BINDING_HOLDINGS_POSITION = HOLDINGS_NOTE_POSITION + 1;
  private static final int NOTE_HOLDINGS_POSITION = HOLDINGS_NOTE_POSITION + 2;

  @Mock
  private ItemReferenceService itemReferenceService;
  @Mock
  private HoldingsReferenceService holdingsReferenceService;

  @InjectMocks
  private NoteTableUpdater noteTableUpdater;

  @Test
  void shouldExtendTableWithItemNoteTypes() {
    var table = UnifiedTableHeaderBuilder.getEmptyTableWithHeaders(Item.class);
    var row = Arrays.stream(new String[table.getHeader().size()]).collect(Collectors.toCollection(ArrayList::new));
    row.set(ITEM_NOTE_POSITION, "Action note;Action note text;false|Note;Note text;false|Binding;Binding text;false");
    table.setRows(List.of(new Row().row(row)));

    var expectedTableSize = table.getHeader().size() + 2;

    when(itemReferenceService.getAllItemNoteTypes())
      .thenReturn(List.of(NoteType.builder().name("Action note").build(),
        NoteType.builder().name("Note").build(),
        NoteType.builder().name("Binding").build()));

    noteTableUpdater.extendTableWithItemNotesTypes(table);

    assertThat(table.getHeader(), hasSize(expectedTableSize));
    var headerNames = table.getHeader().stream().map(Cell::getValue).toList();
    assertThat(headerNames.indexOf("Action note"), is(ACTION_NOTE_POSITION));
    assertThat(headerNames.indexOf("Note"), is(NOTE_POSITION));
    assertThat(headerNames.indexOf("Binding"), is(BINDING_POSITION));

    var modifiedRow = table.getRows().get(0).getRow();
    assertThat(modifiedRow, hasSize(expectedTableSize));
    assertEquals("Action note text", modifiedRow.get(ACTION_NOTE_POSITION));
    assertEquals("Note text", modifiedRow.get(NOTE_POSITION));
    assertEquals("Binding text", modifiedRow.get(BINDING_POSITION));
  }

  @Test
  void shouldExtendTableWithEmptyItemsNotes() {
    var table = UnifiedTableHeaderBuilder.getEmptyTableWithHeaders(Item.class);
    var row = Arrays.stream(new String[table.getHeader().size()]).collect(Collectors.toCollection(ArrayList::new));
    table.setRows(List.of(new Row().row(row)));

    var expectedTableSize = table.getHeader().size() + 2;

    when(itemReferenceService.getAllItemNoteTypes())
      .thenReturn(List.of(NoteType.builder().name("Action note").build(),
        NoteType.builder().name("Note").build(),
        NoteType.builder().name("Binding").build()));

    noteTableUpdater.extendTableWithItemNotesTypes(table);

    assertThat(table.getHeader(), hasSize(expectedTableSize));
    var headerNames = table.getHeader().stream().map(Cell::getValue).toList();
    assertThat(headerNames.indexOf("Action note"), is(ACTION_NOTE_POSITION));
    assertThat(headerNames.indexOf("Note"), is(NOTE_POSITION));
    assertThat(headerNames.indexOf("Binding"), is(BINDING_POSITION));

    var modifiedRow = table.getRows().get(0).getRow();
    assertThat(modifiedRow, hasSize(expectedTableSize));
    assertNull(modifiedRow.get(ACTION_NOTE_POSITION));
    assertNull(modifiedRow.get(NOTE_POSITION));
    assertNull(modifiedRow.get(BINDING_POSITION));
  }

  @Test
  void shouldAddStaffOnlyPostfixWhenRequiredForItemNotes() {
    var table = UnifiedTableHeaderBuilder.getEmptyTableWithHeaders(Item.class);
    var row = Arrays.stream(new String[table.getHeader().size()]).collect(Collectors.toCollection(ArrayList::new));
    row.set(ITEM_NOTE_POSITION, "Action note;Action note text;true|Note;Note text;false|Binding;Binding text;true");
    table.setRows(List.of(new Row().row(row)));

    var expectedTableSize = table.getHeader().size() + 2;

    when(itemReferenceService.getAllItemNoteTypes())
      .thenReturn(List.of(NoteType.builder().name("Action note").build(),
        NoteType.builder().name("Note").build(),
        NoteType.builder().name("Binding").build()));

    noteTableUpdater.extendTableWithItemNotesTypes(table);

    assertThat(table.getHeader(), hasSize(expectedTableSize));
    var headerNames = table.getHeader().stream().map(Cell::getValue).toList();
    assertThat(headerNames.indexOf("Action note"), is(ACTION_NOTE_POSITION));
    assertThat(headerNames.indexOf("Note"), is(NOTE_POSITION));
    assertThat(headerNames.indexOf("Binding"), is(BINDING_POSITION));

    var modifiedRow = table.getRows().get(0).getRow();
    assertThat(modifiedRow, hasSize(expectedTableSize));
    assertTrue(modifiedRow.get(ACTION_NOTE_POSITION).contains(STAFF_ONLY));
    assertFalse(modifiedRow.get(NOTE_POSITION).contains(STAFF_ONLY));
    assertTrue(modifiedRow.get(BINDING_POSITION).contains(STAFF_ONLY));
  }

  @Test
  void shouldExtendTableWithHoldingsNoteTypes() {
    var table = UnifiedTableHeaderBuilder.getEmptyTableWithHeaders(Item.class);
    var row = Arrays.stream(new String[table.getHeader().size()]).collect(Collectors.toCollection(ArrayList::new));
    row.set(HOLDINGS_NOTE_POSITION, "Action note;Action note text;false|Note;Note text;false|Binding;Binding text;false");
    table.setRows(List.of(new Row().row(row)));

    var expectedTableSize = table.getHeader().size() + 2;

    when(holdingsReferenceService.getAllHoldingsNoteTypes())
      .thenReturn(List.of(HoldingsNoteType.builder().name("Action note").build(),
        HoldingsNoteType.builder().name("Note").build(),
        HoldingsNoteType.builder().name("Binding").build()));

    noteTableUpdater.extendTableWithHoldingsNotesTypes(table);

    assertThat(table.getHeader(), hasSize(expectedTableSize));
    var headerNames = table.getHeader().stream().map(Cell::getValue).toList();
    assertThat(headerNames.indexOf("Action note"), is(ACTION_HOLDINGS_NOTE_POSITION));
    assertThat(headerNames.indexOf("Note"), is(NOTE_HOLDINGS_POSITION));
    assertThat(headerNames.indexOf("Binding"), is(BINDING_HOLDINGS_POSITION));

    var modifiedRow = table.getRows().get(0).getRow();
    assertThat(modifiedRow, hasSize(expectedTableSize));
    assertEquals("Action note text", modifiedRow.get(ACTION_HOLDINGS_NOTE_POSITION));
    assertEquals("Note text", modifiedRow.get(NOTE_HOLDINGS_POSITION));
    assertEquals("Binding text", modifiedRow.get(BINDING_HOLDINGS_POSITION));
  }


  @Test
  void shouldExtendTableWithEmptyHoldingsNotes() {
    var table = UnifiedTableHeaderBuilder.getEmptyTableWithHeaders(Item.class);
    var row = Arrays.stream(new String[table.getHeader().size()]).collect(Collectors.toCollection(ArrayList::new));
    table.setRows(List.of(new Row().row(row)));

    var expectedTableSize = table.getHeader().size() + 2;

    when(holdingsReferenceService.getAllHoldingsNoteTypes())
      .thenReturn(List.of(HoldingsNoteType.builder().name("Action note").build(),
        HoldingsNoteType.builder().name("Note").build(),
        HoldingsNoteType.builder().name("Binding").build()));

    noteTableUpdater.extendTableWithHoldingsNotesTypes(table);

    assertThat(table.getHeader(), hasSize(expectedTableSize));
    var headerNames = table.getHeader().stream().map(Cell::getValue).toList();
    assertThat(headerNames.indexOf("Action note"), is(ACTION_HOLDINGS_NOTE_POSITION));
    assertThat(headerNames.indexOf("Note"), is(NOTE_HOLDINGS_POSITION));
    assertThat(headerNames.indexOf("Binding"), is(BINDING_HOLDINGS_POSITION));

    var modifiedRow = table.getRows().get(0).getRow();
    assertThat(modifiedRow, hasSize(expectedTableSize));
    assertNull(modifiedRow.get(ACTION_HOLDINGS_NOTE_POSITION));
    assertNull(modifiedRow.get(NOTE_HOLDINGS_POSITION));
    assertNull(modifiedRow.get(BINDING_HOLDINGS_POSITION));
  }

  @Test
  void shouldAddStaffOnlyPostfixWhenRequiredForHoldingsNotes() {
    var table = UnifiedTableHeaderBuilder.getEmptyTableWithHeaders(Item.class);
    var row = Arrays.stream(new String[table.getHeader().size()]).collect(Collectors.toCollection(ArrayList::new));
    row.set(HOLDINGS_NOTE_POSITION, "Action note;Action note text;true|Note;Note text;false|Binding;Binding text;true");
    table.setRows(List.of(new Row().row(row)));

    var expectedTableSize = table.getHeader().size() + 2;

    when(holdingsReferenceService.getAllHoldingsNoteTypes())
      .thenReturn(List.of(HoldingsNoteType.builder().name("Action note").build(),
        HoldingsNoteType.builder().name("Note").build(),
        HoldingsNoteType.builder().name("Binding").build()));

    noteTableUpdater.extendTableWithHoldingsNotesTypes(table);

    assertThat(table.getHeader(), hasSize(expectedTableSize));
    var headerNames = table.getHeader().stream().map(Cell::getValue).toList();
    assertThat(headerNames.indexOf("Action note"), is(ACTION_HOLDINGS_NOTE_POSITION));
    assertThat(headerNames.indexOf("Note"), is(NOTE_HOLDINGS_POSITION));
    assertThat(headerNames.indexOf("Binding"), is(BINDING_HOLDINGS_POSITION));

    var modifiedRow = table.getRows().get(0).getRow();
    assertThat(modifiedRow, hasSize(expectedTableSize));
    assertTrue(modifiedRow.get(ACTION_HOLDINGS_NOTE_POSITION).contains(STAFF_ONLY));
    assertFalse(modifiedRow.get(NOTE_HOLDINGS_POSITION).contains(STAFF_ONLY));
    assertTrue(modifiedRow.get(BINDING_HOLDINGS_POSITION).contains(STAFF_ONLY));
  }
}
