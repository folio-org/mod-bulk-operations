package org.folio.bulkops.domain.converter;

import static org.folio.bulkops.domain.format.SpecialCharacterEscaper.escape;
import static org.folio.bulkops.domain.format.SpecialCharacterEscaper.restore;
import static org.folio.bulkops.util.Constants.ARRAY_DELIMITER;
import static org.folio.bulkops.util.Constants.ITEM_DELIMITER;
import static org.folio.bulkops.util.Constants.ITEM_DELIMITER_PATTERN;
import static org.folio.bulkops.util.Utils.booleanToStringNullSafe;

import org.apache.commons.lang3.ObjectUtils;
import org.folio.bulkops.domain.bean.InstanceNote;
import org.folio.bulkops.exception.EntityFormatException;
import org.folio.bulkops.service.InstanceReferenceHelper;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class InstanceNoteListConverter extends BaseConverter<List<InstanceNote>> {
  private static final int NUMBER_OF_INSTANCE_NOTE_ELEMENTS = 3;
  private static final int INSTANCE_NOTE_NOTE_TYPE_INDEX = 0;
  private static final int INSTANCE_NOTE_NOTE_INDEX = 1;
  private static final int INSTANCE_NOTE_STAFF_ONLY_INDEX = 2;

  @Override
  public List<InstanceNote> convertToObject(String value) {
    return Arrays.stream(value.split(ITEM_DELIMITER_PATTERN))
      .map(this::restoreInstanceNote)
      .filter(ObjectUtils::isNotEmpty)
      .toList();
  }

  @Override
  public String convertToString(List<InstanceNote> object) {
    return object.stream()
      .filter(Objects::nonNull)
      .map(note -> String.join(ARRAY_DELIMITER,
        escape(InstanceReferenceHelper.service().getNoteTypeNameById(note.getInstanceNoteTypeId())),
        escape(note.getNote()),
        booleanToStringNullSafe(note.getStaffOnly())))
      .collect(Collectors.joining(ITEM_DELIMITER));
  }

  private InstanceNote restoreInstanceNote(String s) {
    if (ObjectUtils.isEmpty(s)) {
      return null;
    }
    var tokens = s.split(ARRAY_DELIMITER, -1);
    if (tokens.length < NUMBER_OF_INSTANCE_NOTE_ELEMENTS) {
      throw new EntityFormatException(String.format("Illegal number of instance note elements: %d, expected: %d", tokens.length,
        NUMBER_OF_INSTANCE_NOTE_ELEMENTS));
    }
    return InstanceNote.builder()
      .instanceNoteTypeId(InstanceReferenceHelper.service().getNoteTypeIdByName(restore(tokens[INSTANCE_NOTE_NOTE_TYPE_INDEX])))
      .note(restore(tokens[INSTANCE_NOTE_NOTE_INDEX]))
      .staffOnly(ObjectUtils.isEmpty(tokens[INSTANCE_NOTE_STAFF_ONLY_INDEX]) ? null : Boolean.parseBoolean(tokens[INSTANCE_NOTE_STAFF_ONLY_INDEX]))
      .build();
  }
}
