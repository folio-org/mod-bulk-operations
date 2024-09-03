package org.folio.bulkops.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.folio.bulkops.BaseTest;
import org.folio.bulkops.domain.bean.HoldingsNoteType;
import org.folio.bulkops.domain.dto.EntityType;
import org.folio.bulkops.domain.entity.BulkOperation;
import org.folio.bulkops.processor.note.AbstractNoteProcessor;
import org.folio.bulkops.processor.note.NoteProcessorFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.List;

class HoldingsNoteProcessorTest extends BaseTest {
  @MockBean
  private HoldingsReferenceService holdingsReferenceService;
  @MockBean
  private ConsortiaService consortiaService;
  @Autowired
  private AbstractNoteProcessor holdingsNotesProcessor;
  @Autowired
  private NoteProcessorFactory noteProcessorFactory;

  @Test
  void shouldEnrichPreviewWithHoldingsRecordsNoteTypeColumns() {
    var sourceCsv = "Holdings UUID,\"Instance (Title, Publisher, Publication date)\",Suppress from discovery,Holdings HRID,Source,Former holdings Id,Holdings type,Statistical codes,Administrative note,Holdings permanent location,Holdings temporary location,Shelving title,Holdings copy number,Holdings level call number type,Holdings level call number prefix,Holdings level call number,Holdings level call number suffix,Number of items,Holdings statement,Holdings statement for supplements,Holdings statement for indexes,ILL policy,Digitization policy,Retention policy,Notes,Electronic access,Acquisition method,Order format,Receipt status,Tags\n" +
      "59b36165-fcf2-49d2-bf7f-25fedbc07e44,Sample instance;123,,ho14,FOLIO,,,,,Main Library,,,,,,,,,,,,,,,Note type 3;note3;false|Note type 1;note1;true|Note type 2;note2;false,,,,,";

    when(consortiaService.isCurrentTenantCentralTenant(any())).thenReturn(false);
    when(holdingsReferenceService.getAllHoldingsNoteTypes(any()))
      .thenReturn(List.of(new HoldingsNoteType().withName("Note type 3"),
        new HoldingsNoteType().withName("Note type 1"),
        new HoldingsNoteType().withName("Note type 2")));

    var res = noteProcessorFactory.getNoteProcessor(EntityType.HOLDINGS_RECORD.getValue()).processNotes(sourceCsv.getBytes(), new BulkOperation());

    var lines = new String(res).split("\n");
    assertThat(lines).hasSize(2);
    var headers = lines[0];
    assertThat(headers).contains("Note type 1,Note type 2,Note type 3");
    var data = lines[1];
    assertThat(data).contains("59b36165-fcf2-49d2-bf7f-25fedbc07e44,Sample instance;123,,ho14,FOLIO,,,,,Main Library,,,,,,,,,,,,,,,,,,,,,,");
  }
}
