package org.folio.bulkops.processor.note;

import static java.util.Optional.ofNullable;
import static org.folio.bulkops.util.Constants.ITEM_NOTE_POSITION;
import static org.folio.spring.utils.FolioExecutionContextUtils.prepareContextForTenant;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.bulkops.domain.bean.NoteType;
import org.folio.bulkops.domain.entity.BulkOperation;
import org.folio.bulkops.service.ItemReferenceService;
import org.folio.spring.FolioModuleMetadata;
import org.folio.spring.scope.FolioExecutionContextSetter;
import org.springframework.cache.Cache;
import org.springframework.stereotype.Component;

@Log4j2
@Component
@AllArgsConstructor
public class ItemNoteProcessor extends CsvDownloadPreProcessor {

  private final ItemReferenceService itemReferenceService;
  private final FolioModuleMetadata folioModuleMetadata;

  @Override
  public List<String> getNoteTypeNames(BulkOperation bulkOperation) {
    var noteTypeNamesSet =
        new HashSet<>(
            itemReferenceService.getAllItemNoteTypes(folioExecutionContext.getTenantId()).stream()
                .map(NoteType::getName)
                .filter(Objects::nonNull)
                .toList());
    if (consortiaService.isTenantCentral(folioExecutionContext.getTenantId())) {
      noteTypeNamesSet.clear();
      List<NoteType> noteTypesFromUsedTenants = new ArrayList<>();
      var usedTenants = bulkOperation.getUsedTenants();
      for (var usedTenant : usedTenants) {
        try (var ignored =
            new FolioExecutionContextSetter(
                prepareContextForTenant(usedTenant, folioModuleMetadata, folioExecutionContext))) {
          var noteTypesFromUsedTenant = itemReferenceService.getAllItemNoteTypes(usedTenant);
          ofNullable(cacheManager.getCache("itemNoteTypes")).ifPresent(Cache::invalidate);
          noteTypesFromUsedTenants.addAll(noteTypesFromUsedTenant);
        }
      }
      noteTableUpdater.updateNoteTypeNamesWithTenants(noteTypesFromUsedTenants);
      noteTypeNamesSet.addAll(
          noteTypesFromUsedTenants.stream().map(NoteType::getName).collect(Collectors.toSet()));
    }
    return noteTypeNamesSet.stream().sorted().toList();
  }

  @Override
  public int getNotePosition() {
    return ITEM_NOTE_POSITION;
  }
}
