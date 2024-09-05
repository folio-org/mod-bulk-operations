package org.folio.bulkops.service;

import static java.lang.Boolean.TRUE;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
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
import org.folio.bulkops.domain.dto.TenantNotePair;
import org.folio.bulkops.domain.dto.UnifiedTable;

import org.folio.bulkops.domain.entity.BulkOperation;
import org.folio.bulkops.domain.format.SpecialCharacterEscaper;
import org.folio.bulkops.repository.BulkOperationRepository;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.FolioModuleMetadata;
import org.folio.spring.scope.FolioExecutionContextSetter;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class NoteTableUpdater {
  private static final int NON_EXISTING_POSITION = -1;
  private static final int NUMBER_OF_NOTE_FIELDS = 5;
  private static final int NOTE_TYPE_POS = 0;
  private static final int NOTE_VALUE_POS = 1;
  private static final int STAFF_ONLY_FLAG_POS = 2;
  private static final int TENANT_POS = 3;

  private final ItemReferenceService itemReferenceService;
  private final HoldingsReferenceService holdingsReferenceService;
  private final InstanceReferenceService instanceReferenceService;
  private final FolioModuleMetadata folioModuleMetadata;
  private final FolioExecutionContext folioExecutionContext;
  private final ConsortiaService consortiaService;
  private final BulkOperationRepository bulkOperationRepository;
  private final CacheManager cacheManager;

  public void extendTableWithHoldingsNotesTypes(UnifiedTable unifiedTable, Set<String> forceVisible, BulkOperation bulkOperation) {
    var noteTypeNamesSet = new HashSet<>(holdingsReferenceService.getAllHoldingsNoteTypes(folioExecutionContext.getTenantId()).stream()
      .map(HoldingsNoteType::getName)
      .toList());
    List<HoldingsNoteType> noteTypesFromUsedTenants = new ArrayList<>();
    List<TenantNotePair> tenantNotePairs = new ArrayList<>();
    if (consortiaService.isCurrentTenantCentralTenant(folioExecutionContext.getTenantId())) {
      noteTypeNamesSet.clear();
      var usedTenants = getUsedTenants(unifiedTable, bulkOperation, HOLDINGS_NOTE_POSITION);
      for (var usedTenant : usedTenants) {
        try (var ignored = new FolioExecutionContextSetter(prepareContextForTenant(usedTenant, folioModuleMetadata, folioExecutionContext))) {
          var noteTypesFromUsedTenant = holdingsReferenceService.getAllHoldingsNoteTypes(usedTenant);
          ofNullable(cacheManager.getCache("holdingsNoteTypes")).ifPresent(Cache::invalidate);
          noteTypesFromUsedTenants.addAll(noteTypesFromUsedTenant);
        }
      }
      var noteTypes = noteTypesFromUsedTenants.stream().map(note -> new NoteType().withName(note.getName())
        .withTenantId(note.getTenantId()).withId(note.getId())).toList();
      updateNoteTypeNamesWithTenants(noteTypes);
      tenantNotePairs.addAll(getTenantNotePairs(bulkOperation, noteTypes));
      noteTypeNamesSet.addAll(noteTypes.stream().map(NoteType::getName).collect(Collectors.toSet()));
    }
    var noteTypeNames = noteTypeNamesSet.stream().sorted().toList();
    extendHeadersWithNoteTypeNames(HOLDINGS_NOTE_POSITION, unifiedTable.getHeader(), noteTypeNames, forceVisible);
    unifiedTable.getRows().forEach(row -> row.setRow(enrichWithNotesByType(row.getRow(), HOLDINGS_NOTE_POSITION, noteTypeNames, tenantNotePairs)));
  }

  public void extendTableWithItemNotesTypes(UnifiedTable unifiedTable, Set<String> forceVisible, BulkOperation bulkOperation) {
    var noteTypeNamesSet = new HashSet<>(itemReferenceService.getAllItemNoteTypes(folioExecutionContext.getTenantId()).stream()
      .map(NoteType::getName)
      .toList());
    List<NoteType> noteTypesFromUsedTenants = new ArrayList<>();
    List<TenantNotePair> tenantNotePairs = new ArrayList<>();
    if (consortiaService.isCurrentTenantCentralTenant(folioExecutionContext.getTenantId())) {
      noteTypeNamesSet.clear();
      var usedTenants = getUsedTenants(unifiedTable, bulkOperation, ITEM_NOTE_POSITION);
      for (var usedTenant : usedTenants) {
        try (var ignored = new FolioExecutionContextSetter(prepareContextForTenant(usedTenant, folioModuleMetadata, folioExecutionContext))) {
          var noteTypesFromUsedTenant = itemReferenceService.getAllItemNoteTypes(usedTenant);
          ofNullable(cacheManager.getCache("itemNoteTypes")).ifPresent(Cache::invalidate);
          noteTypesFromUsedTenants.addAll(noteTypesFromUsedTenant);
        }
      }
      updateNoteTypeNamesWithTenants(noteTypesFromUsedTenants);
      tenantNotePairs.addAll(getTenantNotePairs(bulkOperation, noteTypesFromUsedTenants));
      noteTypeNamesSet.addAll(noteTypesFromUsedTenants.stream().map(NoteType::getName).collect(Collectors.toSet()));
    }
    var noteTypeNames = noteTypeNamesSet.stream().sorted().toList();
    extendHeadersWithNoteTypeNames(ITEM_NOTE_POSITION, unifiedTable.getHeader(), noteTypeNames, forceVisible);
    unifiedTable.getRows().forEach(row -> row.setRow(enrichWithNotesByType(row.getRow(), ITEM_NOTE_POSITION, noteTypeNames, tenantNotePairs)));
  }

  public void extendTableWithInstanceNotesTypes(UnifiedTable unifiedTable, Set<String> forceVisible) {
    var noteTypeNames = instanceReferenceService.getAllInstanceNoteTypes().stream()
      .map(InstanceNoteType::getName)
      .sorted()
      .toList();

    if (!noteTypeNames.isEmpty()) {
      extendHeadersWithNoteTypeNames(INSTANCE_NOTE_POSITION, unifiedTable.getHeader(), noteTypeNames, forceVisible);
      unifiedTable.getRows().forEach(row -> row.setRow(enrichWithNotesByType(row.getRow(), INSTANCE_NOTE_POSITION, noteTypeNames, Collections.emptyList())));
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

  public List<String> enrichWithNotesByType(List<String> list, int notesPosition, List<String> noteTypeNames, List<TenantNotePair> tenantNotePairs) {
    var notesArray = new String[noteTypeNames.size()];
    var notesString = list.get(notesPosition);
    if (isNotEmpty(notesString)) {
      for (var note : notesString.split(ITEM_DELIMITER_PATTERN)) {
        var noteFields = note.trim().split(ARRAY_DELIMITER);
        if (noteFields.length == NUMBER_OF_NOTE_FIELDS) {
          var restored = noteFields[NOTE_TYPE_POS];
          if (nonNull(tenantNotePairs) && tenantNotePairs.stream().anyMatch(noteWithTenant -> noteWithTenant.getTenantId().equals(noteFields[TENANT_POS]) &&
            noteWithTenant.getNoteTypeName().equals(noteFields[NOTE_TYPE_POS] + " (" + noteFields[TENANT_POS] + ")"))) {
            restored += " (" + noteFields[TENANT_POS] + ")";
          }
          var position = noteTypeNames.indexOf(SpecialCharacterEscaper.restore(restored));
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

  public List<String> getUsedTenants(UnifiedTable unifiedTable, BulkOperation bulkOperation, int notesPosition) {
    List<String> usedTenants = bulkOperation.getUsedTenants();
    if (isNull(usedTenants)) {
      usedTenants = unifiedTable.getRows().stream().flatMap(row -> Arrays.stream(row.getRow().get(notesPosition)
        .split(ITEM_DELIMITER_PATTERN))).map(items -> items.trim().split(ARRAY_DELIMITER))
        .filter(noteFields -> noteFields.length == NUMBER_OF_NOTE_FIELDS)
        .map(noteFields -> noteFields[TENANT_POS]).distinct().toList();
      bulkOperation.setUsedTenants(usedTenants);
      bulkOperationRepository.save(bulkOperation);
    }
    return usedTenants;
  }

  public void updateNoteTypeNamesWithTenants(List<NoteType> noteTypesFromUsedTenants) {
    noteTypesFromUsedTenants.stream().collect(Collectors.groupingBy(NoteType::getName))
      .values().stream().filter(noteTypes -> noteTypes.stream().map(NoteType::getId).distinct().count() > 1)
      .flatMap(List::stream).distinct().forEach(note -> note.setName(note.getName() + " (" + note.getTenantId() + ")"));
  }

  private List<TenantNotePair> getTenantNotePairs(BulkOperation bulkOperation, List<NoteType> noteTypesFromUsedTenants) {
    var tenantNotePairs = bulkOperation.getTenantNotePairs();
    if (isNull(tenantNotePairs)) {
      tenantNotePairs = noteTypesFromUsedTenants.stream().map(note -> new TenantNotePair().tenantId(note.getTenantId())
        .noteTypeName(note.getName())).toList();
      bulkOperation.setTenantNotePairs(tenantNotePairs);
      bulkOperationRepository.save(bulkOperation);
    }
    return tenantNotePairs;
  }
}
