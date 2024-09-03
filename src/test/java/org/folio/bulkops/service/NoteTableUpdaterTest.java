package org.folio.bulkops.service;

import static java.util.Collections.emptySet;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.folio.bulkops.domain.bean.HoldingsNoteType;
import org.folio.bulkops.domain.bean.HoldingsRecord;
import org.folio.bulkops.domain.bean.Item;
import org.folio.bulkops.domain.bean.NoteType;
import org.folio.bulkops.domain.dto.Cell;
import org.folio.bulkops.domain.dto.Row;
import org.folio.bulkops.domain.entity.BulkOperation;
import org.folio.bulkops.domain.format.SpecialCharacterEscaper;
import org.folio.bulkops.repository.BulkOperationRepository;
import org.folio.bulkops.util.UnifiedTableHeaderBuilder;
import org.folio.spring.FolioExecutionContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

@ExtendWith(MockitoExtension.class)
class NoteTableUpdaterTest {
  private static final int ACTION_NOTE_POSITION = ITEM_NOTE_POSITION;
  private static final int NOTE_POSITION = ITEM_NOTE_POSITION + 1;
  private static final int OTHER_POSITION = ITEM_NOTE_POSITION + 2;

  private static final int ACTION_HOLDINGS_NOTE_POSITION = HOLDINGS_NOTE_POSITION;
  private static final int NOTE_HOLDINGS_POSITION = HOLDINGS_NOTE_POSITION + 1;
  private static final int OTHER_HOLDINGS_POSITION = HOLDINGS_NOTE_POSITION + 2;

  @Mock
  private FolioExecutionContext folioExecutionContext;
  @Mock
  private ConsortiaService consortiaService;
  @Mock
  private ItemReferenceService itemReferenceService;
  @Mock
  private HoldingsReferenceService holdingsReferenceService;
  @Mock
  private BulkOperationRepository bulkOperationRepository;
  @Mock
  private CacheManager cacheManager;
  @Mock
  private Cache cache;

  @InjectMocks
  private NoteTableUpdater noteTableUpdater;

  @Test
  void shouldExtendTableWithItemNoteTypes() {
    var table = UnifiedTableHeaderBuilder.getEmptyTableWithHeaders(Item.class);
    var row = Arrays.stream(new String[table.getHeader().size()]).collect(Collectors.toCollection(ArrayList::new));
    row.set(ITEM_NOTE_POSITION, "Action note;Action note text;false;tenant;3e095251-4e9a-4484-8473-d5580abeccd0|Note;Note text;false;tenant;3e095251-4e9a-4484-8473-d5580abeccd0|Other;Other text;false;tenant;3e095251-4e9a-4484-8473-d5580abeccd0");
    table.setRows(List.of(new Row().row(row)));

    var expectedTableSize = table.getHeader().size() + 2;

    when(folioExecutionContext.getTenantId()).thenReturn("tenantId");
    when(consortiaService.isCurrentTenantCentralTenant("tenantId")).thenReturn(false);
    when(itemReferenceService.getAllItemNoteTypes("tenantId"))
      .thenReturn(List.of(NoteType.builder().name("Action note").build(),
        NoteType.builder().name("Note").build(),
        NoteType.builder().name("Other").build()));

    noteTableUpdater.extendTableWithItemNotesTypes(table, Set.of("Action note", "Other"), null);

    assertThat(table.getHeader(), hasSize(expectedTableSize));
    var headerNames = table.getHeader().stream().map(Cell::getValue).toList();
    assertThat(headerNames.indexOf("Action note"), is(ACTION_NOTE_POSITION));
    assertThat(headerNames.indexOf("Note"), is(NOTE_POSITION));
    assertThat(headerNames.indexOf("Other"), is(OTHER_POSITION));

    assertTrue(table.getHeader().get(ACTION_NOTE_POSITION).getForceVisible());
    assertFalse(table.getHeader().get(NOTE_POSITION).getForceVisible());
    assertTrue(table.getHeader().get(OTHER_POSITION).getForceVisible());

    var modifiedRow = table.getRows().get(0).getRow();
    assertThat(modifiedRow, hasSize(expectedTableSize));
    assertEquals("Action note text", modifiedRow.get(ACTION_NOTE_POSITION));
    assertEquals("Note text", modifiedRow.get(NOTE_POSITION));
    assertEquals("Other text", modifiedRow.get(OTHER_POSITION));
  }

  @Test
  void shouldExtendTableWithItemNoteTypesInConsortia() {
    Map<String, Collection<String>> headers = new HashMap<>();
    headers.put("tenant", List.of("central"));
    var table = UnifiedTableHeaderBuilder.getEmptyTableWithHeaders(Item.class);
    var row = Arrays.stream(new String[table.getHeader().size()]).collect(Collectors.toCollection(ArrayList::new));
    row.set(ITEM_NOTE_POSITION, "Action note;Action note text;false;member;60b1f73e-bbf2-4807-806b-3166620a7aaa|Note;Note text;false;member;60b1f73e-bbf2-4807-806b-3166620a7aaa|Other;Other text;false;member;60b1f73e-bbf2-4807-806b-3166620a7aaa");
    table.setRows(List.of(new Row().row(row)));

    var expectedTableSize = table.getHeader().size() + 2;

    when(consortiaService.isCurrentTenantCentralTenant("central")).thenReturn(true);
    when(folioExecutionContext.getTenantId()).thenReturn("central");
    when(folioExecutionContext.getOkapiHeaders()).thenReturn(headers);
    when(itemReferenceService.getAllItemNoteTypes("central")).thenReturn(List.of());
    when(itemReferenceService.getAllItemNoteTypes("member"))
      .thenReturn(List.of(NoteType.builder().name("Action note").tenantId("member").build(),
        NoteType.builder().name("Note").tenantId("member").build(),
        NoteType.builder().name("Other").tenantId("member").build()));
    when(cacheManager.getCache("itemNoteTypes")).thenReturn(cache);

    noteTableUpdater.extendTableWithItemNotesTypes(table, Set.of("Action note", "Other"), new BulkOperation());

    assertThat(table.getHeader(), hasSize(expectedTableSize));
    var headerNames = table.getHeader().stream().map(Cell::getValue).toList();
    assertThat(headerNames.indexOf("Action note"), is(ACTION_NOTE_POSITION));
    assertThat(headerNames.indexOf("Note"), is(NOTE_POSITION));
    assertThat(headerNames.indexOf("Other"), is(OTHER_POSITION));

    assertTrue(table.getHeader().get(ACTION_NOTE_POSITION).getForceVisible());
    assertFalse(table.getHeader().get(NOTE_POSITION).getForceVisible());
    assertTrue(table.getHeader().get(OTHER_POSITION).getForceVisible());

    var modifiedRow = table.getRows().get(0).getRow();
    assertThat(modifiedRow, hasSize(expectedTableSize));
    assertEquals("Action note text", modifiedRow.get(ACTION_NOTE_POSITION));
    assertEquals("Note text", modifiedRow.get(NOTE_POSITION));
    assertEquals("Other text", modifiedRow.get(OTHER_POSITION));
  }

  @Test
  void shouldExtendTableWithEmptyItemsNotes() {
    var table = UnifiedTableHeaderBuilder.getEmptyTableWithHeaders(Item.class);
    var row = Arrays.stream(new String[table.getHeader().size()]).collect(Collectors.toCollection(ArrayList::new));
    table.setRows(List.of(new Row().row(row)));

    var expectedTableSize = table.getHeader().size() + 2;

    when(folioExecutionContext.getTenantId()).thenReturn("tenantId");
    when(consortiaService.isCurrentTenantCentralTenant("tenantId")).thenReturn(false);
    when(itemReferenceService.getAllItemNoteTypes("tenantId"))
      .thenReturn(List.of(NoteType.builder().name("Action note").build(),
        NoteType.builder().name("Note").build(),
        NoteType.builder().name("Other").build()));

    noteTableUpdater.extendTableWithItemNotesTypes(table, emptySet(), null);

    assertThat(table.getHeader(), hasSize(expectedTableSize));
    var headerNames = table.getHeader().stream().map(Cell::getValue).toList();
    assertThat(headerNames.indexOf("Action note"), is(ACTION_NOTE_POSITION));
    assertThat(headerNames.indexOf("Note"), is(NOTE_POSITION));
    assertThat(headerNames.indexOf("Other"), is(OTHER_POSITION));

    var modifiedRow = table.getRows().get(0).getRow();
    assertThat(modifiedRow, hasSize(expectedTableSize));
    assertNull(modifiedRow.get(ACTION_NOTE_POSITION));
    assertNull(modifiedRow.get(NOTE_POSITION));
    assertNull(modifiedRow.get(OTHER_POSITION));
  }

  @Test
  void shouldAddStaffOnlyPostfixWhenRequiredForItemNotes() {
    var table = UnifiedTableHeaderBuilder.getEmptyTableWithHeaders(Item.class);
    var row = Arrays.stream(new String[table.getHeader().size()]).collect(Collectors.toCollection(ArrayList::new));
    row.set(ITEM_NOTE_POSITION, "Action note;Action note text;true;tenantId;60b1f73e-bbf2-4807-806b-3166620a7aaa|Note;Note text;false;tenantId;60b1f73e-bbf2-4807-806b-3166620a7aaa|Other;Other text;true;tenantId;60b1f73e-bbf2-4807-806b-3166620a7aaa");
    table.setRows(List.of(new Row().row(row)));

    var expectedTableSize = table.getHeader().size() + 2;

    when(folioExecutionContext.getTenantId()).thenReturn("tenantId");
    when(consortiaService.isCurrentTenantCentralTenant("tenantId")).thenReturn(false);
    when(itemReferenceService.getAllItemNoteTypes("tenantId"))
      .thenReturn(List.of(NoteType.builder().name("Action note").build(),
        NoteType.builder().name("Note").build(),
        NoteType.builder().name("Other").build()));

    noteTableUpdater.extendTableWithItemNotesTypes(table, emptySet(), null);

    assertThat(table.getHeader(), hasSize(expectedTableSize));
    var headerNames = table.getHeader().stream().map(Cell::getValue).toList();
    assertThat(headerNames.indexOf("Action note"), is(ACTION_NOTE_POSITION));
    assertThat(headerNames.indexOf("Note"), is(NOTE_POSITION));
    assertThat(headerNames.indexOf("Other"), is(OTHER_POSITION));

    var modifiedRow = table.getRows().get(0).getRow();
    assertThat(modifiedRow, hasSize(expectedTableSize));
    assertTrue(modifiedRow.get(ACTION_NOTE_POSITION).contains(STAFF_ONLY));
    assertFalse(modifiedRow.get(NOTE_POSITION).contains(STAFF_ONLY));
    assertTrue(modifiedRow.get(OTHER_POSITION).contains(STAFF_ONLY));
  }

  @Test
  void shouldExtendTableWithHoldingsNoteTypes() {
    var table = UnifiedTableHeaderBuilder.getEmptyTableWithHeaders(Item.class);
    var row = Arrays.stream(new String[table.getHeader().size()]).collect(Collectors.toCollection(ArrayList::new));
    row.set(HOLDINGS_NOTE_POSITION, "Action note;Action note text;false;tenant;60b1f73e-bbf2-4807-806b-3166620a7aaa|Note;Note text;false;tenant;60b1f73e-bbf2-4807-806b-3166620a7aaa|Other;Other text;false;tenant;60b1f73e-bbf2-4807-806b-3166620a7aaa");
    table.setRows(List.of(new Row().row(row)));

    var expectedTableSize = table.getHeader().size() + 2;

    when(folioExecutionContext.getTenantId()).thenReturn("tenantId");
    when(consortiaService.isCurrentTenantCentralTenant("tenantId")).thenReturn(false);
    when(holdingsReferenceService.getAllHoldingsNoteTypes("tenantId"))
      .thenReturn(List.of(HoldingsNoteType.builder().name("Action note").build(),
        HoldingsNoteType.builder().name("Note").build(),
        HoldingsNoteType.builder().name("Other").build()));

    noteTableUpdater.extendTableWithHoldingsNotesTypes(table, Set.of("Action note", "Other"), null);

    assertThat(table.getHeader(), hasSize(expectedTableSize));
    var headerNames = table.getHeader().stream().map(Cell::getValue).toList();
    assertThat(headerNames.indexOf("Action note"), is(ACTION_HOLDINGS_NOTE_POSITION));
    assertThat(headerNames.indexOf("Note"), is(NOTE_HOLDINGS_POSITION));
    assertThat(headerNames.indexOf("Other"), is(OTHER_HOLDINGS_POSITION));

    assertTrue(table.getHeader().get(ACTION_HOLDINGS_NOTE_POSITION).getForceVisible());
    assertFalse(table.getHeader().get(NOTE_HOLDINGS_POSITION).getForceVisible());
    assertTrue(table.getHeader().get(OTHER_HOLDINGS_POSITION).getForceVisible());

    var modifiedRow = table.getRows().get(0).getRow();
    assertThat(modifiedRow, hasSize(expectedTableSize));
    assertEquals("Action note text", modifiedRow.get(ACTION_HOLDINGS_NOTE_POSITION));
    assertEquals("Note text", modifiedRow.get(NOTE_HOLDINGS_POSITION));
    assertEquals("Other text", modifiedRow.get(OTHER_HOLDINGS_POSITION));
  }

  @Test
  void shouldExtendTableWithHoldingsNoteTypesInConsortia() {
    Map<String, Collection<String>> headers = new HashMap<>();
    headers.put("tenant", List.of("central"));
    var table = UnifiedTableHeaderBuilder.getEmptyTableWithHeaders(Item.class);
    var row = Arrays.stream(new String[table.getHeader().size()]).collect(Collectors.toCollection(ArrayList::new));
    row.set(HOLDINGS_NOTE_POSITION, "Action note;Action note text;false;member;3e095251-4e9a-4484-8473-d5580abeccd0|Note;Note text;false;member;3e095251-4e9a-4484-8473-d5580abeccd0|Other;Other text;false;member;3e095251-4e9a-4484-8473-d5580abeccd0");
    table.setRows(List.of(new Row().row(row)));

    var expectedTableSize = table.getHeader().size() + 2;

    when(consortiaService.isCurrentTenantCentralTenant("central")).thenReturn(true);
    when(folioExecutionContext.getTenantId()).thenReturn("central");
    when(folioExecutionContext.getOkapiHeaders()).thenReturn(headers);
    when(holdingsReferenceService.getAllHoldingsNoteTypes("central")).thenReturn(List.of());
    when(holdingsReferenceService.getAllHoldingsNoteTypes("member"))
      .thenReturn(List.of(HoldingsNoteType.builder().name("Action note").build(),
        HoldingsNoteType.builder().name("Note").build(),
        HoldingsNoteType.builder().name("Other").build()));
    when(cacheManager.getCache("holdingsNoteTypes")).thenReturn(cache);

    noteTableUpdater.extendTableWithHoldingsNotesTypes(table, Set.of("Action note", "Other"), new BulkOperation());

    assertThat(table.getHeader(), hasSize(expectedTableSize));
    var headerNames = table.getHeader().stream().map(Cell::getValue).toList();
    assertThat(headerNames.indexOf("Action note"), is(ACTION_HOLDINGS_NOTE_POSITION));
    assertThat(headerNames.indexOf("Note"), is(NOTE_HOLDINGS_POSITION));
    assertThat(headerNames.indexOf("Other"), is(OTHER_HOLDINGS_POSITION));

    assertTrue(table.getHeader().get(ACTION_HOLDINGS_NOTE_POSITION).getForceVisible());
    assertFalse(table.getHeader().get(NOTE_HOLDINGS_POSITION).getForceVisible());
    assertTrue(table.getHeader().get(OTHER_HOLDINGS_POSITION).getForceVisible());

    var modifiedRow = table.getRows().get(0).getRow();
    assertThat(modifiedRow, hasSize(expectedTableSize));
    assertEquals("Action note text", modifiedRow.get(ACTION_HOLDINGS_NOTE_POSITION));
    assertEquals("Note text", modifiedRow.get(NOTE_HOLDINGS_POSITION));
    assertEquals("Other text", modifiedRow.get(OTHER_HOLDINGS_POSITION));
  }

  @Test
  void shouldExtendTableWithEmptyHoldingsNotes() {
    var table = UnifiedTableHeaderBuilder.getEmptyTableWithHeaders(Item.class);
    var row = Arrays.stream(new String[table.getHeader().size()]).collect(Collectors.toCollection(ArrayList::new));
    table.setRows(List.of(new Row().row(row)));

    var expectedTableSize = table.getHeader().size() + 2;

    when(folioExecutionContext.getTenantId()).thenReturn("tenantId");
    when(consortiaService.isCurrentTenantCentralTenant("tenantId")).thenReturn(false);
    when(holdingsReferenceService.getAllHoldingsNoteTypes("tenantId"))
      .thenReturn(List.of(HoldingsNoteType.builder().name("Action note").build(),
        HoldingsNoteType.builder().name("Note").build(),
        HoldingsNoteType.builder().name("Other").build()));

    noteTableUpdater.extendTableWithHoldingsNotesTypes(table, emptySet(), null);

    assertThat(table.getHeader(), hasSize(expectedTableSize));
    var headerNames = table.getHeader().stream().map(Cell::getValue).toList();
    assertThat(headerNames.indexOf("Action note"), is(ACTION_HOLDINGS_NOTE_POSITION));
    assertThat(headerNames.indexOf("Note"), is(NOTE_HOLDINGS_POSITION));
    assertThat(headerNames.indexOf("Other"), is(OTHER_HOLDINGS_POSITION));

    var modifiedRow = table.getRows().get(0).getRow();
    assertThat(modifiedRow, hasSize(expectedTableSize));
    assertNull(modifiedRow.get(ACTION_HOLDINGS_NOTE_POSITION));
    assertNull(modifiedRow.get(NOTE_HOLDINGS_POSITION));
    assertNull(modifiedRow.get(OTHER_HOLDINGS_POSITION));
  }

  @Test
  void shouldAddStaffOnlyPostfixWhenRequiredForHoldingsNotes() {
    var table = UnifiedTableHeaderBuilder.getEmptyTableWithHeaders(Item.class);
    var row = Arrays.stream(new String[table.getHeader().size()]).collect(Collectors.toCollection(ArrayList::new));
    row.set(HOLDINGS_NOTE_POSITION, "Action note;Action note text;true;tenantId;3e095251-4e9a-4484-8473-d5580abeccd0|Note;Note text;false;tenantId;3e095251-4e9a-4484-8473-d5580abeccd0|Other;Other text;true;tenantId;3e095251-4e9a-4484-8473-d5580abeccd0");
    table.setRows(List.of(new Row().row(row)));

    var expectedTableSize = table.getHeader().size() + 2;

    when(folioExecutionContext.getTenantId()).thenReturn("tenantId");
    when(consortiaService.isCurrentTenantCentralTenant("tenantId")).thenReturn(false);
    when(holdingsReferenceService.getAllHoldingsNoteTypes("tenantId"))
      .thenReturn(List.of(HoldingsNoteType.builder().name("Action note").tenantId("tenantId").build(),
        HoldingsNoteType.builder().name("Note").tenantId("tenantId").build(),
        HoldingsNoteType.builder().name("Other").tenantId("tenantId").build()));

    noteTableUpdater.extendTableWithHoldingsNotesTypes(table, emptySet(), new BulkOperation());

    assertThat(table.getHeader(), hasSize(expectedTableSize));
    var headerNames = table.getHeader().stream().map(Cell::getValue).toList();
    assertThat(headerNames.indexOf("Action note"), is(ACTION_HOLDINGS_NOTE_POSITION));
    assertThat(headerNames.indexOf("Note"), is(NOTE_HOLDINGS_POSITION));
    assertThat(headerNames.indexOf("Other"), is(OTHER_HOLDINGS_POSITION));

    var modifiedRow = table.getRows().get(0).getRow();
    assertThat(modifiedRow, hasSize(expectedTableSize));
    assertTrue(modifiedRow.get(ACTION_HOLDINGS_NOTE_POSITION).contains(STAFF_ONLY));
    assertFalse(modifiedRow.get(NOTE_HOLDINGS_POSITION).contains(STAFF_ONLY));
    assertTrue(modifiedRow.get(OTHER_HOLDINGS_POSITION).contains(STAFF_ONLY));
  }

  @Test
  void shouldAddNotePostfixWhenRequiredForHoldingsNoteTypeNames() {
    var table = UnifiedTableHeaderBuilder.getEmptyTableWithHeaders(HoldingsRecord.class);
    var row = Arrays.stream(new String[table.getHeader().size()]).collect(Collectors.toCollection(ArrayList::new));
    table.setRows(List.of(new Row().row(row)));

    when(folioExecutionContext.getTenantId()).thenReturn("tenantId");
    when(consortiaService.isCurrentTenantCentralTenant("tenantId")).thenReturn(false);
    when(holdingsReferenceService.getAllHoldingsNoteTypes("tenantId"))
      .thenReturn(List.of(HoldingsNoteType.builder().name("Binding").build(),
        HoldingsNoteType.builder().name("Electronic bookplate").build(),
        HoldingsNoteType.builder().name("Provenance").build(),
        HoldingsNoteType.builder().name("Reproduction").build()));

    noteTableUpdater.extendTableWithHoldingsNotesTypes(table, emptySet(), null);

    var headerNames = table.getHeader().stream().map(Cell::getValue).toList();
    List.of("Binding", "Electronic bookplate", "Provenance", "Reproduction").forEach(name ->
      assertTrue(headerNames.contains(name + " note")));
  }

  @Test
  void shouldAddNotePostfixWhenRequiredForItemNoteTypeNames() {
    var table = UnifiedTableHeaderBuilder.getEmptyTableWithHeaders(Item.class);
    var row = Arrays.stream(new String[table.getHeader().size()]).collect(Collectors.toCollection(ArrayList::new));
    table.setRows(List.of(new Row().row(row)));

    when(folioExecutionContext.getTenantId()).thenReturn("tenantId");
    when(consortiaService.isCurrentTenantCentralTenant("tenantId")).thenReturn(false);
    when(itemReferenceService.getAllItemNoteTypes("tenantId"))
      .thenReturn(List.of(NoteType.builder().name("Binding").build(),
        NoteType.builder().name("Electronic bookplate").build(),
        NoteType.builder().name("Provenance").build(),
        NoteType.builder().name("Reproduction").build()));

    noteTableUpdater.extendTableWithItemNotesTypes(table, emptySet(), null);

    var headerNames = table.getHeader().stream().map(Cell::getValue).toList();
    List.of("Binding", "Electronic bookplate", "Provenance", "Reproduction").forEach(name ->
      assertTrue(headerNames.contains(name + " note")));
  }

  @Test
  void shouldEnrichWithNotesByTypeIfNoteTypeHasSpecialCharacters() {
    var noteTypeWithEscapedSpecialCharacters = SpecialCharacterEscaper.escape("O|;:ther");
    var row = new ArrayList<>(List.of("Note;Note text;false;tenant;6c8e4b97-4224-4155-8b13-07e29aca88ad|" + noteTypeWithEscapedSpecialCharacters + ";Other text;true;tenant;6c8e4b97-4224-4155-8b13-07e29aca88ad"));
    var noteTypeNames = List.of("Note", "O|;:ther");
    var enriched = noteTableUpdater.enrichWithNotesByType(row, 0, noteTypeNames, Collections.emptyList());
    assertEquals(2, enriched.size());
    assertEquals("Other text (staff only)", enriched.get(1));
  }
}
