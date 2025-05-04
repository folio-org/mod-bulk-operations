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
import org.folio.bulkops.domain.bean.ItemNote;
import org.folio.bulkops.exception.NotFoundException;
import org.folio.bulkops.service.ItemReferenceHelper;

@Log4j2
public class ItemNoteListConverter extends BaseConverter<List<ItemNote>> {

  @Override
  public String convertToString(List<ItemNote> object) {
    return  object.stream()
      .filter(Objects::nonNull)
      .map(itemNote -> {
        var noteTypeName = itemNote.getItemNoteTypeName();
        if (isNull(noteTypeName)) {
          noteTypeName = "";
          try {
            noteTypeName = ItemReferenceHelper.service().getNoteTypeNameById(itemNote.getItemNoteTypeId(), itemNote.getTenantId());
          } catch (NotFoundException e) {
            log.error("Item note type with id = {} not found : {}", itemNote.getItemNoteTypeId(), e.getMessage());
          }
        }
          return String.join(ARRAY_DELIMITER,
            escape(noteTypeName),
            escape(itemNote.getNote()),
            escape(booleanToStringNullSafe(itemNote.getStaffOnly())),
            itemNote.getTenantId(),
            itemNote.getItemNoteTypeId());
      })
      .collect(Collectors.joining(ITEM_DELIMITER));
  }
}
