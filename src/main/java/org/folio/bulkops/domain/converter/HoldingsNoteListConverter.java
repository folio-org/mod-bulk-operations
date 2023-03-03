package org.folio.bulkops.domain.converter;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.folio.bulkops.domain.format.SpecialCharacterEscaper.escape;
import static org.folio.bulkops.domain.format.SpecialCharacterEscaper.restore;
import static org.folio.bulkops.util.Constants.ARRAY_DELIMITER;
import static org.folio.bulkops.util.Constants.ITEM_DELIMITER;
import static org.folio.bulkops.util.Constants.ITEM_DELIMITER_PATTERN;
import static org.folio.bulkops.util.Utils.booleanToStringNullSafe;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ObjectUtils;
import org.folio.bulkops.domain.bean.HoldingsNote;
import org.folio.bulkops.exception.EntityFormatException;
import org.folio.bulkops.service.HoldingsReferenceHelper;

import com.opencsv.bean.AbstractBeanField;
import com.opencsv.exceptions.CsvConstraintViolationException;
import com.opencsv.exceptions.CsvDataTypeMismatchException;

public class HoldingsNoteListConverter extends AbstractBeanField<String, List<HoldingsNote>> {
  private static final int NUMBER_OF_HOLDINGS_NOTE_ELEMENTS = 3;
  private static final int HOLDINGS_NOTE_NOTE_TYPE_INDEX = 0;
  private static final int HOLDINGS_NOTE_NOTE_INDEX = 1;
  private static final int HOLDINGS_NOTE_STAFF_ONLY_INDEX = 2;

  @Override
  protected Object convert(String value) throws CsvDataTypeMismatchException, CsvConstraintViolationException {
    return ObjectUtils.isEmpty(value) ?
      Collections.emptyList() :
      Arrays.stream(value.split(ITEM_DELIMITER_PATTERN))
        .map(this::restoreHoldingsNote)
        .filter(ObjectUtils::isNotEmpty)
        .toList();
  }

  @Override
  protected String convertToWrite(Object value) {
    return ObjectUtils.isEmpty(value) ?
      EMPTY :
      ((List<HoldingsNote>) value).stream()
        .filter(Objects::nonNull)
        .map(note -> String.join(ARRAY_DELIMITER,
          escape(HoldingsReferenceHelper.service().getNoteTypeNameById(note.getHoldingsNoteTypeId())),
          escape(note.getNote()),
          booleanToStringNullSafe(note.getStaffOnly())))
        .collect(Collectors.joining(ITEM_DELIMITER));
  }

  private HoldingsNote restoreHoldingsNote(String s) {
    if (ObjectUtils.isEmpty(s)) {
      return null;
    }
    var tokens = s.split(ARRAY_DELIMITER, -1);
    if (tokens.length < NUMBER_OF_HOLDINGS_NOTE_ELEMENTS) {
      throw new EntityFormatException(String.format("Illegal number of holdings note elements: %d, expected: %d", tokens.length,
        NUMBER_OF_HOLDINGS_NOTE_ELEMENTS));
    }
    return HoldingsNote.builder()
      .holdingsNoteTypeId(HoldingsReferenceHelper.service().getNoteTypeIdByName(restore(tokens[HOLDINGS_NOTE_NOTE_TYPE_INDEX])))
      .note(restore(tokens[HOLDINGS_NOTE_NOTE_INDEX]))
      .staffOnly(ObjectUtils.isEmpty(tokens[HOLDINGS_NOTE_STAFF_ONLY_INDEX]) ? null : Boolean.parseBoolean(tokens[HOLDINGS_NOTE_STAFF_ONLY_INDEX]))
      .build();
  }
}
