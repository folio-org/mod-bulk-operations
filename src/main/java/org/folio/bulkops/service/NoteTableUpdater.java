package org.folio.bulkops.service;

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
import static org.folio.spring.utils.FolioExecutionContextUtils.prepareContextForTenant;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.bulkops.client.SearchConsortium;
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

@Component
@RequiredArgsConstructor
@Log4j2
public class NoteTableUpdater {
  private static final int NON_EXISTING_POSITION = -1;
  private static final int NUMBER_OF_NOTE_FIELDS_FOR_HOLDINGS_AND_ITEMS = 5;
  private static final int NUMBER_OF_NOTE_FIELDS_FOR_INSTANCES = 3;
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
  private final SearchConsortium searchConsortium;

  public void extendTableWithHoldingsNotesTypes(
      UnifiedTable unifiedTable, Set<String> forceVisible, BulkOperation bulkOperation) {
    var noteTypeNamesSet =
        new HashSet<>(
            holdingsReferenceService
                .getAllHoldingsNoteTypes(folioExecutionContext.getTenantId())
                .stream()
                .map(HoldingsNoteType::getName)
                .toList());
    List<HoldingsNoteType> noteTypesFromUsedTenants = new ArrayList<>();
    List<TenantNotePair> tenantNotePairs = new ArrayList<>();
    if (consortiaService.isTenantCentral(folioExecutionContext.getTenantId())) {
      noteTypeNamesSet.clear();
      for (var usedTenant : bulkOperation.getUsedTenants()) {
        try (var ignored =
            new FolioExecutionContextSetter(
                prepareContextForTenant(usedTenant, folioModuleMetadata, folioExecutionContext))) {
          var noteTypesFromUsedTenant =
              holdingsReferenceService.getAllHoldingsNoteTypes(usedTenant);
          ofNullable(cacheManager.getCache("holdingsNoteTypes")).ifPresent(Cache::invalidate);
          noteTypesFromUsedTenants.addAll(noteTypesFromUsedTenant);
        }
      }
      var noteTypes =
          noteTypesFromUsedTenants.stream()
              .map(
                  note ->
                      new NoteType()
                          .withName(note.getName())
                          .withTenantId(note.getTenantId())
                          .withId(note.getId()))
              .toList();
      updateNoteTypeNamesWithTenants(noteTypes);
      tenantNotePairs.addAll(getTenantNotePairs(bulkOperation, noteTypes));
      noteTypeNamesSet.addAll(
          noteTypes.stream().map(NoteType::getName).collect(Collectors.toSet()));
    }
    var noteTypeNames = noteTypeNamesSet.stream().sorted().toList();
    extendHeadersWithNoteTypeNames(
        HOLDINGS_NOTE_POSITION, unifiedTable.getHeader(), noteTypeNames, forceVisible);
    unifiedTable
        .getRows()
        .forEach(
            row ->
                row.setRow(
                    enrichWithNotesByType(
                        row.getRow(),
                        HOLDINGS_NOTE_POSITION,
                        noteTypeNames,
                        tenantNotePairs,
                        false)));
  }

  public void extendTableWithItemNotesTypes(
      UnifiedTable unifiedTable, Set<String> forceVisible, BulkOperation bulkOperation) {
    var noteTypeNamesSet =
        new HashSet<>(
            itemReferenceService.getAllItemNoteTypes(folioExecutionContext.getTenantId()).stream()
                .map(NoteType::getName)
                .toList());
    List<NoteType> noteTypesFromUsedTenants = new ArrayList<>();
    List<TenantNotePair> tenantNotePairs = new ArrayList<>();
    if (consortiaService.isTenantCentral(folioExecutionContext.getTenantId())) {
      noteTypeNamesSet.clear();
      for (var usedTenant : bulkOperation.getUsedTenants()) {
        try (var ignored =
            new FolioExecutionContextSetter(
                prepareContextForTenant(usedTenant, folioModuleMetadata, folioExecutionContext))) {
          var noteTypesFromUsedTenant = itemReferenceService.getAllItemNoteTypes(usedTenant);
          ofNullable(cacheManager.getCache("itemNoteTypes")).ifPresent(Cache::invalidate);
          noteTypesFromUsedTenants.addAll(noteTypesFromUsedTenant);
        }
      }
      updateNoteTypeNamesWithTenants(noteTypesFromUsedTenants);
      tenantNotePairs.addAll(getTenantNotePairs(bulkOperation, noteTypesFromUsedTenants));
      noteTypeNamesSet.addAll(
          noteTypesFromUsedTenants.stream().map(NoteType::getName).collect(Collectors.toSet()));
    }
    var noteTypeNames = noteTypeNamesSet.stream().sorted().toList();
    extendHeadersWithNoteTypeNames(
        ITEM_NOTE_POSITION, unifiedTable.getHeader(), noteTypeNames, forceVisible);
    unifiedTable
        .getRows()
        .forEach(
            row ->
                row.setRow(
                    enrichWithNotesByType(
                        row.getRow(), ITEM_NOTE_POSITION, noteTypeNames, tenantNotePairs, false)));
  }

  public void extendTableWithInstanceNotesTypes(
      UnifiedTable unifiedTable, Set<String> forceVisible) {
    var noteTypeNames =
        instanceReferenceService.getAllInstanceNoteTypes().stream()
            .map(InstanceNoteType::getName)
            .sorted()
            .toList();

    if (!noteTypeNames.isEmpty()) {
      extendHeadersWithNoteTypeNames(
          INSTANCE_NOTE_POSITION, unifiedTable.getHeader(), noteTypeNames, forceVisible);
      unifiedTable
          .getRows()
          .forEach(
              row ->
                  row.setRow(
                      enrichWithNotesByType(
                          row.getRow(), INSTANCE_NOTE_POSITION, noteTypeNames, null, true)));
    }
  }

  public String concatNotePostfixIfRequired(String noteTypeName) {
    return Set.of("Binding", "Electronic bookplate", "Provenance", "Reproduction")
            .contains(noteTypeName)
        ? noteTypeName + " note"
        : noteTypeName;
  }

  public void extendHeadersWithNoteTypeNames(
      int notesInitialPosition,
      List<Cell> headers,
      List<String> noteTypeNames,
      Set<String> forceVisible) {
    var headerToReplace = headers.get(notesInitialPosition);
    var cellsToInsert =
        noteTypeNames.stream()
            .map(
                name ->
                    new Cell()
                        .value(concatNotePostfixIfRequired(name))
                        .visible(headerToReplace.getVisible())
                        .forceVisible(forceVisible.contains(name))
                        .dataType(headerToReplace.getDataType())
                        .ignoreTranslation(true))
            .toList();
    headers.remove(notesInitialPosition);
    headers.addAll(notesInitialPosition, cellsToInsert);
  }

  public List<String> enrichWithNotesByType(
      List<String> list,
      int notesPosition,
      List<String> noteTypeNames,
      List<TenantNotePair> tenantNotePairs,
      boolean forInstances) {
    var notesArray = new String[noteTypeNames.size()];
    var notesString = list.get(notesPosition);
    var numOfNoteFields =
        !forInstances
            ? NUMBER_OF_NOTE_FIELDS_FOR_HOLDINGS_AND_ITEMS
            : NUMBER_OF_NOTE_FIELDS_FOR_INSTANCES;
    if (isNotEmpty(notesString)) {
      for (var note : notesString.split(ITEM_DELIMITER_PATTERN)) {
        var noteFields = note.trim().split(ARRAY_DELIMITER);
        if (noteFields.length == numOfNoteFields) {
          var restored = noteFields[NOTE_TYPE_POS];
          if (nonNull(tenantNotePairs)
              && tenantNotePairs.stream()
                  .anyMatch(
                      noteWithTenant ->
                          noteWithTenant.getTenantId().equals(noteFields[TENANT_POS])
                              && noteWithTenant
                                  .getNoteTypeName()
                                  .equals(
                                      noteFields[NOTE_TYPE_POS]
                                          + " ("
                                          + noteFields[TENANT_POS]
                                          + ")"))) {
            restored += " (" + noteFields[TENANT_POS] + ")";
          }
          var position = noteTypeNames.indexOf(SpecialCharacterEscaper.restore(restored));
          if (position != NON_EXISTING_POSITION) {
            var staffOnlyPostfix =
                Boolean.parseBoolean(noteFields[STAFF_ONLY_FLAG_POS]) ? SPACE + STAFF_ONLY : EMPTY;
            var value =
                SpecialCharacterEscaper.restore(noteFields[NOTE_VALUE_POS]) + staffOnlyPostfix;
            notesArray[position] =
                isEmpty(notesArray[position])
                    ? value
                    : String.join(ITEM_DELIMITER_SPACED, notesArray[position], value);
          }
        }
      }
    }
    list.remove(notesPosition);
    list.addAll(notesPosition, Arrays.asList(notesArray));
    return list;
  }

  public void updateNoteTypeNamesWithTenants(List<NoteType> noteTypesFromUsedTenants) {
    var numOfAllTenants =
        noteTypesFromUsedTenants.stream().map(NoteType::getTenantId).distinct().count();
    var noteTypesPresentInAllTenants =
        noteTypesFromUsedTenants.stream()
            .collect(Collectors.groupingBy(noteType -> noteType.getId() + noteType.getName()))
            .values()
            .stream()
            .filter(
                noteTypes -> noteTypes.stream().map(NoteType::getName).count() == numOfAllTenants)
            .flatMap(List::stream)
            .collect(Collectors.toSet());
    noteTypesFromUsedTenants.stream()
        .filter(noteType -> !noteTypesPresentInAllTenants.contains(noteType))
        .distinct()
        .forEach(note -> note.setName(note.getName() + " (" + note.getTenantId() + ")"));
  }

  private List<TenantNotePair> getTenantNotePairs(
      BulkOperation bulkOperation, List<NoteType> noteTypesFromUsedTenants) {
    var tenantNotePairs = bulkOperation.getTenantNotePairs();
    if (isNull(tenantNotePairs)) {
      tenantNotePairs =
          noteTypesFromUsedTenants.stream()
              .map(
                  note ->
                      new TenantNotePair()
                          .tenantId(note.getTenantId())
                          .noteTypeName(note.getName())
                          .noteTypeId(note.getId()))
              .toList();
      bulkOperation.setTenantNotePairs(tenantNotePairs);
      bulkOperationRepository.save(bulkOperation);
    }
    return tenantNotePairs;
  }
}
