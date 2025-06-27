package org.folio.bulkops.domain.converter;

import static java.util.Objects.isNull;
import static org.folio.bulkops.domain.format.SpecialCharacterEscaper.escape;
import static org.folio.bulkops.util.Constants.ARRAY_DELIMITER;
import static org.folio.bulkops.util.Constants.ITEM_DELIMITER;
import static org.folio.bulkops.util.Utils.booleanToStringNullSafe;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import lombok.extern.log4j.Log4j2;
import org.folio.bulkops.domain.bean.HoldingsNote;
import org.folio.bulkops.exception.NotFoundException;
import org.folio.bulkops.service.HoldingsReferenceHelper;

@Log4j2
public class HoldingsNoteListConverter extends BaseConverter<List<HoldingsNote>> {

  @Override
  public String convertToString(List<HoldingsNote> object) {
    return object.stream()
      .filter(Objects::nonNull)
      .map(note -> {
        var noteTypeName = note.getHoldingsNoteTypeName();
        if (isNull(noteTypeName)) {
          noteTypeName = "";
          try {
            noteTypeName = HoldingsReferenceHelper.service().getNoteTypeNameById(note.getHoldingsNoteTypeId(), note.getTenantId());
          } catch (NotFoundException e) {
            log.error("Holding note type with id = {} not found : {}", note.getHoldingsNoteTypeId(), e.getMessage());
          }
        }
        return String.join(ARRAY_DELIMITER,
          escape(noteTypeName),
          escape(note.getNote()),
          booleanToStringNullSafe(note.getStaffOnly()),
          note.getTenantId(),
          note.getHoldingsNoteTypeId());
      }).collect(Collectors.joining(ITEM_DELIMITER));
  }
}
