package org.folio.bulkops.processor.note;

import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.bulkops.domain.bean.HoldingsNoteType;
import org.folio.bulkops.domain.bean.NoteType;
import org.folio.bulkops.domain.entity.BulkOperation;
import org.folio.bulkops.service.ConsortiaService;
import org.folio.bulkops.service.HoldingsReferenceService;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.FolioModuleMetadata;
import org.folio.spring.scope.FolioExecutionContextSetter;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.folio.bulkops.util.Constants.HOLDINGS_NOTE_POSITION;
import static org.folio.bulkops.util.FolioExecutionContextUtil.prepareContextForTenant;

@Component
@Log4j2
@AllArgsConstructor
public class HoldingsNotesProcessor extends AbstractNoteProcessor {

  private final HoldingsReferenceService holdingsReferenceService;
  private final FolioExecutionContext folioExecutionContext;
  private final FolioModuleMetadata folioModuleMetadata;
  private final ConsortiaService consortiaService;

  @Override
  public List<String> getNoteTypeNames(BulkOperation bulkOperation) {
    var noteTypeNamesSet = new HashSet<>(holdingsReferenceService.getAllHoldingsNoteTypes(folioExecutionContext.getTenantId()).stream()
      .map(HoldingsNoteType::getName)
      .filter(Objects::nonNull)
      .toList());
    if (consortiaService.isCurrentTenantCentralTenant(folioExecutionContext.getTenantId())) {
      noteTypeNamesSet.clear();
      List<HoldingsNoteType> noteTypesFromUsedTenants = new ArrayList<>();
      var usedTenants = bulkOperation.getUsedTenants();
      for (var usedTenant : usedTenants) {
        try (var ignored = new FolioExecutionContextSetter(prepareContextForTenant(usedTenant, folioModuleMetadata, folioExecutionContext))) {
          var noteTypesFromUsedTenant = holdingsReferenceService.getAllHoldingsNoteTypes(usedTenant);
          cacheManager.getCache("holdingsNoteTypes").invalidate();
          noteTypesFromUsedTenants.addAll(noteTypesFromUsedTenant);
        }
      }
      var noteTypes = noteTypesFromUsedTenants.stream().map(note -> new NoteType().withName(note.getName())
        .withTenantId(note.getTenantId()).withId(note.getId())).toList();
      noteTableUpdater.updateNoteTypesFromUsedTenants(noteTypes);
      noteTypeNamesSet.addAll(noteTypes.stream().map(NoteType::getName).collect(Collectors.toSet()));
    }
    return noteTypeNamesSet.stream().sorted().toList();
  }

  @Override
  public int getNotePosition() {
    return HOLDINGS_NOTE_POSITION;
  }
}
