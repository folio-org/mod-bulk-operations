package org.folio.bulkops.domain.converter;

import static org.apache.commons.lang3.ObjectUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.SPACE;
import static org.folio.bulkops.domain.format.SpecialCharacterEscaper.escape;
import static org.folio.bulkops.util.Constants.ITEM_DELIMITER_PATTERN;
import static org.folio.bulkops.util.Constants.ITEM_DELIMITER_SPACED;
import static org.folio.bulkops.util.Constants.STAFF_ONLY;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.folio.bulkops.domain.bean.CirculationNote;

public class CirculationNoteListConverter extends BaseConverter<List<CirculationNote>> {
  @Override
  public List<CirculationNote> convertToObject(String value) {
    return Arrays.stream(value.split(ITEM_DELIMITER_PATTERN))
      .map(String::trim)
      .map(this::restoreCirculationNote)
      .filter(Objects::nonNull)
      .toList();
  }

  @Override
  public String convertToString(List<CirculationNote> object) {
    return object.stream()
      .filter(Objects::nonNull)
      .map(this::circulationNotesToString)
      .collect(Collectors.joining(ITEM_DELIMITER_SPACED));
  }

  private CirculationNote restoreCirculationNote(String s) {
    var note = s.endsWith(STAFF_ONLY) ? s.substring(0, s.lastIndexOf(STAFF_ONLY)).trim() : s;
    return isEmpty(s) ? null :
      CirculationNote.builder()
        .staffOnly(s.endsWith(STAFF_ONLY))
        .note(note)
        .build();
  }

  private String circulationNotesToString(CirculationNote note) {
    return escape(note.getNote() + (Boolean.TRUE.equals(note.getStaffOnly()) ? SPACE + STAFF_ONLY : EMPTY));
  }
}
