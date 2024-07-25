package org.folio.bulkops.service;

import static java.lang.Boolean.TRUE;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.SPACE;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.folio.bulkops.util.Constants.ARRAY_DELIMITER;
import static org.folio.bulkops.util.Constants.HOLDINGS_NOTE_POSITION;
import static org.folio.bulkops.util.Constants.INSTANCE_NOTE_POSITION;
import static org.folio.bulkops.util.Constants.ITEM_DELIMITER_PATTERN;
import static org.folio.bulkops.util.Constants.ITEM_DELIMITER_SPACED;
import static org.folio.bulkops.util.Constants.ITEM_NOTE_POSITION;
import static org.folio.bulkops.util.Constants.STAFF_ONLY;
import static org.folio.bulkops.util.FolioExecutionContextUtil.prepareContextForTenant;

import lombok.RequiredArgsConstructor;
import org.folio.bulkops.domain.bean.HoldingsNoteType;
import org.folio.bulkops.domain.bean.NoteType;
import org.folio.bulkops.domain.dto.Cell;
import org.folio.bulkops.domain.dto.InstanceNoteType;
import org.folio.bulkops.domain.dto.UnifiedTable;

import org.folio.bulkops.domain.format.SpecialCharacterEscaper;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.FolioModuleMetadata;
import org.folio.spring.scope.FolioExecutionContextSetter;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class NoteTableUpdater {
  private static final int NON_EXISTING_POSITION = -1;
  private static final int NUMBER_OF_NOTE_FIELDS = 3;
  private static final int NOTE_TYPE_POS = 0;
  private static final int NOTE_VALUE_POS = 1;
  private static final int STAFF_ONLY_FLAG_POS = 2;

  private final ItemReferenceService itemReferenceService;
  private final HoldingsReferenceService holdingsReferenceService;
  private final InstanceReferenceService instanceReferenceService;
  private final FolioModuleMetadata folioModuleMetadata;
  private final FolioExecutionContext folioExecutionContext;
  private final ConsortiaService consortiaService;

  public void extendTableWithHoldingsNotesTypes(UnifiedTable unifiedTable, Set<String> forceVisible) {
    var noteTypeNamesSet = new HashSet<>(holdingsReferenceService.getAllHoldingsNoteTypes(folioExecutionContext.getTenantId()).stream()
      .map(HoldingsNoteType::getName)
      .toList());
    if (consortiaService.isCurrentTenantCentralTenant(folioExecutionContext.getTenantId())) {
      var userTenants = consortiaService.getAffiliatedTenants(folioExecutionContext.getTenantId(), folioExecutionContext.getUserId().toString());
      for (var userTenant : userTenants) {
        try (var ignored = new FolioExecutionContextSetter(prepareContextForTenant(userTenant, folioModuleMetadata, folioExecutionContext))) {
          var noteTypesFromMember = holdingsReferenceService.getAllHoldingsNoteTypes(userTenant).stream()
            .map(HoldingsNoteType::getName)
            .toList();
          noteTypeNamesSet.addAll(noteTypesFromMember);
        }
      }
    }
    var noteTypeNames = noteTypeNamesSet.stream().sorted().toList();
    extendHeadersWithNoteTypeNames(HOLDINGS_NOTE_POSITION, unifiedTable.getHeader(), noteTypeNames, forceVisible);
    unifiedTable.getRows().forEach(row -> row.setRow(enrichWithNotesByType(row.getRow(), HOLDINGS_NOTE_POSITION, noteTypeNames)));
  }

  public void extendTableWithItemNotesTypes(UnifiedTable unifiedTable, Set<String> forceVisible) {
    var noteTypeNamesSet = new HashSet<>(itemReferenceService.getAllItemNoteTypes(folioExecutionContext.getTenantId()).stream()
      .map(NoteType::getName)
      .toList());
    if (consortiaService.isCurrentTenantCentralTenant(folioExecutionContext.getTenantId())) {
      var userTenants = consortiaService.getAffiliatedTenants(folioExecutionContext.getTenantId(), folioExecutionContext.getUserId().toString());
      for (var userTenant : userTenants) {
        try (var ignored = new FolioExecutionContextSetter(prepareContextForTenant(userTenant, folioModuleMetadata, folioExecutionContext))) {
          var noteTypesFromMember = itemReferenceService.getAllItemNoteTypes(userTenant).stream()
            .map(NoteType::getName)
            .toList();
          noteTypeNamesSet.addAll(noteTypesFromMember);
        }
      }
    }
    var noteTypeNames = noteTypeNamesSet.stream().sorted().toList();
    extendHeadersWithNoteTypeNames(ITEM_NOTE_POSITION, unifiedTable.getHeader(), noteTypeNames, forceVisible);
    unifiedTable.getRows().forEach(row -> row.setRow(enrichWithNotesByType(row.getRow(), ITEM_NOTE_POSITION, noteTypeNames)));
  }

  public void extendTableWithInstanceNotesTypes(UnifiedTable unifiedTable, Set<String> forceVisible) {
    var noteTypeNames = instanceReferenceService.getAllInstanceNoteTypes().stream()
      .map(InstanceNoteType::getName)
      .sorted()
      .toList();

    if (!noteTypeNames.isEmpty()) {
      extendHeadersWithNoteTypeNames(INSTANCE_NOTE_POSITION, unifiedTable.getHeader(), noteTypeNames, forceVisible);
      unifiedTable.getRows().forEach(row -> row.setRow(enrichWithNotesByType(row.getRow(), INSTANCE_NOTE_POSITION, noteTypeNames)));
    }
  }

  public String concatNotePostfixIfRequired(String noteTypeName) {
    return Set.of("Binding", "Electronic bookplate", "Provenance", "Reproduction").contains(noteTypeName) ?
      noteTypeName + " note" :
      noteTypeName;
  }

  public void extendHeadersWithNoteTypeNames(int notesInitialPosition, List<Cell> headers, List<String> noteTypeNames, Set<String> forceVisible) {
    var headerToReplace = headers.get(notesInitialPosition);
    var cellsToInsert = noteTypeNames.stream()
      .map(name -> new Cell()
        .value(concatNotePostfixIfRequired(name))
        .visible(headerToReplace.getVisible())
        .forceVisible(forceVisible.contains(name))
        .dataType(headerToReplace.getDataType())
        .ignoreTranslation(true))
      .toList();
    headers.remove(notesInitialPosition);
    headers.addAll(notesInitialPosition, cellsToInsert);
  }

  public List<String> enrichWithNotesByType(List<String> list, int notesPosition, List<String> noteTypeNames) {
    var notesArray = new String[noteTypeNames.size()];
    var notesString = list.get(notesPosition);
    if (isNotEmpty(notesString)) {
      for (var note : notesString.split(ITEM_DELIMITER_PATTERN)) {
        var noteFields = note.trim().split(ARRAY_DELIMITER);
        if (noteFields.length == NUMBER_OF_NOTE_FIELDS) {
          var position = noteTypeNames.indexOf(SpecialCharacterEscaper.restore(noteFields[NOTE_TYPE_POS]));
          if (position != NON_EXISTING_POSITION) {
            var staffOnlyPostfix = TRUE.equals(Boolean.parseBoolean(noteFields[STAFF_ONLY_FLAG_POS])) ? SPACE + STAFF_ONLY : EMPTY;
            var value = SpecialCharacterEscaper.restore(noteFields[NOTE_VALUE_POS]) + staffOnlyPostfix;
            notesArray[position] = isEmpty(notesArray[position]) ? value : String.join(ITEM_DELIMITER_SPACED, notesArray[position], value);
          }
        }
      }
    }
    list.remove(notesPosition);
    list.addAll(notesPosition, Arrays.asList(notesArray));
    return list;
  }
}
