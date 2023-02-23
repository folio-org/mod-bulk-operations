package org.folio.bulkops.domain.converter;

import com.opencsv.bean.AbstractBeanField;
import com.opencsv.exceptions.CsvConstraintViolationException;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import org.apache.commons.lang3.ObjectUtils;
import org.folio.bulkops.domain.bean.CirculationNote;
import org.folio.bulkops.domain.bean.Personal;
import org.folio.bulkops.domain.bean.Source;
import org.folio.bulkops.domain.format.SpecialCharacterEscaper;
import org.folio.bulkops.exception.EntityFormatException;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.folio.bulkops.adapters.BulkEditAdapterHelper.dateFromString;
import static org.folio.bulkops.adapters.BulkEditAdapterHelper.dateToString;
import static org.folio.bulkops.domain.format.SpecialCharacterEscaper.escape;
import static org.folio.bulkops.domain.format.SpecialCharacterEscaper.restore;
import static org.folio.bulkops.util.Constants.ARRAY_DELIMITER;
import static org.folio.bulkops.util.Constants.ITEM_DELIMITER;
import static org.folio.bulkops.util.Constants.ITEM_DELIMITER_PATTERN;

public class CirculationNoteListConverter extends AbstractBeanField<String, List<CirculationNote>> {
  private static final int NUMBER_OF_CIRCULATION_NOTE_COMPONENTS = 8;
  private static final int CIRC_NOTE_ID_INDEX = 0;
  private static final int CIRC_NOTE_TYPE_INDEX = 1;
  private static final int CIRC_NOTE_NOTE_INDEX = 2;
  private static final int CIRC_NOTE_STAFF_ONLY_OFFSET = 5;
  private static final int CIRC_NOTE_SOURCE_ID_OFFSET = 4;
  private static final int CIRC_NOTE_LAST_NAME_OFFSET = 3;
  private static final int CIRC_NOTE_FIRST_NAME_OFFSET = 2;
  private static final int CIRC_NOTE_DATE_OFFSET = 1;

  @Override
  protected Object convert(String value) throws CsvDataTypeMismatchException, CsvConstraintViolationException {
    return isEmpty(value) ? Collections.emptyList() :
      Arrays.stream(value.split(ITEM_DELIMITER_PATTERN))
        .map(this::restoreCirculationNote)
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
  }

  @Override
  protected String convertToWrite(Object value) {
    return ObjectUtils.isEmpty(value) ?
      EMPTY :
      ((List<CirculationNote>) value).stream()
        .filter(Objects::nonNull)
        .map(this::circulationNotesToString)
        .collect(Collectors.joining(ITEM_DELIMITER));
  }

  private CirculationNote restoreCirculationNote(String s) {
    if (isNotEmpty(s)) {
      var tokens = s.split(ARRAY_DELIMITER, -1);
      if (tokens.length < NUMBER_OF_CIRCULATION_NOTE_COMPONENTS) {
        throw new EntityFormatException(String.format("Illegal number of circulation note elements: %d, expected: %d", tokens.length, NUMBER_OF_CIRCULATION_NOTE_COMPONENTS));
      }
      return CirculationNote.builder()
        .id(tokens[CIRC_NOTE_ID_INDEX])
        .noteType(CirculationNote.NoteTypeEnum.fromValue(tokens[CIRC_NOTE_TYPE_INDEX]))
        .note(Arrays.stream(tokens, CIRC_NOTE_NOTE_INDEX, tokens.length - CIRC_NOTE_STAFF_ONLY_OFFSET)
          .map(SpecialCharacterEscaper::restore)
          .collect(Collectors.joining(";")))
        .staffOnly(Boolean.valueOf(tokens[tokens.length - CIRC_NOTE_STAFF_ONLY_OFFSET]))
        .source(Source.builder()
          .id(tokens[tokens.length - CIRC_NOTE_SOURCE_ID_OFFSET])
          .personal(Personal.builder()
            .lastName(restore(tokens[tokens.length - CIRC_NOTE_LAST_NAME_OFFSET]))
            .firstName(restore(tokens[tokens.length - CIRC_NOTE_FIRST_NAME_OFFSET]))
            .build())
          .build())
        .date(dateFromString(tokens[tokens.length - CIRC_NOTE_DATE_OFFSET]))
        .build();
    }
    return null;
  }

  private String circulationNotesToString(CirculationNote note) {
    return String.join(ARRAY_DELIMITER,
      note.getId(),
      note.getNoteType().getValue(),
      escape(note.getNote()),
      note.getStaffOnly().toString(),
      ObjectUtils.isEmpty(note.getSource().getId()) ? EMPTY : note.getSource().getId(),
      ObjectUtils.isEmpty(note.getSource().getPersonal().getLastName()) ? EMPTY : escape(note.getSource().getPersonal().getLastName()),
      ObjectUtils.isEmpty(note.getSource().getPersonal().getFirstName()) ? EMPTY : escape(note.getSource().getPersonal().getFirstName()),
      dateToString(note.getDate()));
  }
}
