package org.folio.bulkops.processor.note;

import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.bulkops.domain.bean.HoldingsNoteType;
import org.folio.bulkops.service.ConsortiaService;
import org.folio.bulkops.service.HoldingsReferenceService;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.FolioModuleMetadata;
import org.folio.spring.scope.FolioExecutionContextSetter;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;

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
  public List<String> getNoteTypeNames() {
    var noteTypeNamesSet = new HashSet<>(holdingsReferenceService.getAllHoldingsNoteTypes(folioExecutionContext.getTenantId()).stream()
      .map(HoldingsNoteType::getName)
      .filter(Objects::nonNull)
      .toList());
    if (consortiaService.isCurrentTenantCentralTenant(folioExecutionContext.getTenantId())) {
      var userTenants = consortiaService.getAffiliatedTenants(folioExecutionContext.getTenantId(), folioExecutionContext.getUserId().toString());
      for (var userTenant : userTenants) {
        try (var ignored = new FolioExecutionContextSetter(prepareContextForTenant(userTenant, folioModuleMetadata, folioExecutionContext))) {
          var noteTypesFromMember = holdingsReferenceService.getAllHoldingsNoteTypes(folioExecutionContext.getTenantId()).stream()
            .map(HoldingsNoteType::getName)
            .filter(Objects::nonNull)
            .toList();
          noteTypeNamesSet.addAll(noteTypesFromMember);
        }
      }
    }
    return noteTypeNamesSet.stream().sorted().toList();
  }

  @Override
  public int getNotePosition() {
    return HOLDINGS_NOTE_POSITION;
  }
}
