package org.folio.bulkops.domain.converter;

import static com.opencsv.ICSVWriter.DEFAULT_SEPARATOR;
import static java.util.Objects.isNull;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.SPACE;
import static org.folio.bulkops.domain.bean.CirculationNote.NoteTypeEnum.IN;
import static org.folio.bulkops.domain.bean.CirculationNote.NoteTypeEnum.OUT;
import static org.folio.bulkops.util.Constants.ACTION_NOTE;
import static org.folio.bulkops.util.Constants.BINDING;
import static org.folio.bulkops.util.Constants.COPY_NOTE;
import static org.folio.bulkops.util.Constants.ELECTRONIC_BOOKPLATE;
import static org.folio.bulkops.util.Constants.ITEM_DELIMITER_SPACED;
import static org.folio.bulkops.util.Constants.NOTE;
import static org.folio.bulkops.util.Constants.PROVENANCE;
import static org.folio.bulkops.util.Constants.REPRODUCTION;
import static org.folio.bulkops.util.Constants.STAFF_ONLY;

import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.bean.StatefulBeanToCsvBuilder;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;
import org.folio.bulkops.domain.bean.BulkOperationsEntity;
import org.folio.bulkops.domain.bean.HoldingsNote;
import org.folio.bulkops.domain.bean.HoldingsNoteType;
import org.folio.bulkops.domain.bean.HoldingsRecord;
import org.folio.bulkops.domain.bean.Item;
import org.folio.bulkops.exception.ConverterException;
import org.folio.bulkops.service.HoldingsReferenceHelper;
import org.springframework.util.ObjectUtils;

import java.io.Writer;
import java.util.List;
import java.util.stream.Collectors;

public class BulkOperationsEntityCsvWriter {
  private CustomMappingStrategy<BulkOperationsEntity> strategy;
  private StatefulBeanToCsv<BulkOperationsEntity> delegate;

  public BulkOperationsEntityCsvWriter(Writer writer, Class<? extends BulkOperationsEntity> clazz) {
    strategy = new CustomMappingStrategy<>();
    strategy.setType(clazz);
    delegate = new StatefulBeanToCsvBuilder<BulkOperationsEntity>(writer)
      .withSeparator(DEFAULT_SEPARATOR)
      .withApplyQuotesToAll(false)
      .withMappingStrategy(strategy)
      .build();
  }

  public void write(BulkOperationsEntity entity)
    throws ConverterException, CsvRequiredFieldEmptyException, CsvDataTypeMismatchException {
    delegate.write(processNotes(entity));
  }

  private BulkOperationsEntity processNotes(BulkOperationsEntity entity) {
    if (entity instanceof Item item) {
      return splitCirculationNotes(item);
    } else if (entity instanceof HoldingsRecord holdingsRecord) {
      return splitHoldingsNotes(holdingsRecord);
    }
    return entity;
  }

  private Item splitCirculationNotes(Item item) {
    var notes = item.getCirculationNotes();
    return ObjectUtils.isEmpty(notes) ? item :
      item.withCheckInNotes(notes.stream().filter(circulationNote -> IN.equals(circulationNote.getNoteType())).toList())
        .withCheckOutNotes(notes.stream().filter(circulationNote -> OUT.equals(circulationNote.getNoteType())).toList());
  }

  private HoldingsRecord splitHoldingsNotes(HoldingsRecord holdingsRecord) {
    if (isNotEmpty(holdingsRecord.getNotes())) {
      var notes = holdingsRecord.getNotes();
      var noteTypes = HoldingsReferenceHelper.service().getHoldingsNoteTypes().stream()
        .collect(Collectors.toMap(HoldingsNoteType::getName, HoldingsNoteType::getId, (existing, replacement) -> existing));
      holdingsRecord.setActionNote(fetchNotesByType(notes, noteTypes.get(ACTION_NOTE)));
      holdingsRecord.setBindingNote(fetchNotesByType(notes, noteTypes.get(BINDING)));
      holdingsRecord.setCopyNote(fetchNotesByType(notes, noteTypes.get(COPY_NOTE)));
      holdingsRecord.setElectronicBookplateNote(fetchNotesByType(notes, noteTypes.get(ELECTRONIC_BOOKPLATE)));
      holdingsRecord.setNote(fetchNotesByType(notes, noteTypes.get(NOTE)));
      holdingsRecord.setProvenanceNote(fetchNotesByType(notes, noteTypes.get(PROVENANCE)));
      holdingsRecord.setReproductionNote(fetchNotesByType(notes, noteTypes.get(REPRODUCTION)));
    }
    return holdingsRecord;
  }

  private String fetchNotesByType(List<HoldingsNote> notes, String type) {
    return isNull(type) ?
      EMPTY :
      notes.stream()
        .filter(note -> type.equals(note.getHoldingsNoteTypeId()))
        .map(this::holdingsNoteToString)
        .collect(Collectors.joining(ITEM_DELIMITER_SPACED));
  }

  private String holdingsNoteToString(HoldingsNote holdingsNote) {
    return holdingsNote.getNote() + (Boolean.TRUE.equals(holdingsNote.getStaffOnly()) ?
      SPACE + STAFF_ONLY : EMPTY);
  }
}
