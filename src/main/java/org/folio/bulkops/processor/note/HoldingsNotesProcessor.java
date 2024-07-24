package org.folio.bulkops.processor.note;

import lombok.extern.log4j.Log4j2;
import org.folio.bulkops.domain.bean.HoldingsNoteType;
import org.folio.bulkops.service.HoldingsReferenceService;
import org.folio.bulkops.service.NoteTableUpdater;
import org.folio.spring.FolioExecutionContext;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

import static org.folio.bulkops.util.Constants.HOLDINGS_NOTE_POSITION;

@Component
@Log4j2
public class HoldingsNotesProcessor extends AbstractNoteProcessor {

  private final HoldingsReferenceService holdingsReferenceService;

  public HoldingsNotesProcessor(HoldingsReferenceService holdingsReferenceService, NoteTableUpdater noteTableUpdater, FolioExecutionContext folioExecutionContext) {
    super(noteTableUpdater, folioExecutionContext);
    this.holdingsReferenceService = holdingsReferenceService;
  }

  @Override
  public List<String> getNoteTypeNames() {
    return holdingsReferenceService.getAllHoldingsNoteTypes(folioExecutionContext.getTenantId()).stream()
      .map(HoldingsNoteType::getName)
      .filter(Objects::nonNull)
      .sorted()
      .toList();
  }

  @Override
  public int getNotePosition() {
    return HOLDINGS_NOTE_POSITION;
  }
}
