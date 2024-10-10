package org.folio.bulkops.domain.converter;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.folio.bulkops.domain.format.SpecialCharacterEscaper.escape;
import static org.folio.bulkops.domain.format.SpecialCharacterEscaper.restore;
import static org.folio.bulkops.util.Constants.ARRAY_DELIMITER;
import static org.folio.bulkops.util.Constants.ITEM_DELIMITER;
import static org.folio.bulkops.util.Constants.ITEM_DELIMITER_PATTERN;
import static org.folio.bulkops.util.Utils.booleanToStringNullSafe;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.folio.bulkops.domain.bean.ItemNote;
import org.folio.bulkops.domain.format.SpecialCharacterEscaper;
import org.folio.bulkops.exception.EntityFormatException;
import org.folio.bulkops.exception.NotFoundException;
import org.folio.bulkops.service.ItemReferenceHelper;

public class ItemNoteListConverter extends BaseConverter<List<ItemNote>> {
  private static final int NUMBER_OF_ITEM_NOTE_COMPONENTS = 5;
  private static final int NOTE_TYPE_NAME_INDEX = 0;
  private static final int NOTE_INDEX = 1;
  private static final int TENANT_INDEX = 3;
  private static final int STAFF_ONLY_OFFSET = 3;

  @Override
  public List<ItemNote> convertToObject(String value) {
    return Arrays.stream(value.split(ITEM_DELIMITER_PATTERN))
      .map(this::restoreItemNote)
      .filter(Objects::nonNull)
      .toList();
  }
  @Override
  public String convertToString(List<ItemNote> object) {
    return  object.stream()
      .filter(Objects::nonNull)
      .map(itemNote -> {
        try {
          return String.join(ARRAY_DELIMITER,
            escape(ItemReferenceHelper.service().getNoteTypeNameById(itemNote.getItemNoteTypeId(), itemNote.getTenantId())),
            escape(itemNote.getNote()),
            escape(booleanToStringNullSafe(itemNote.getStaffOnly())),
            itemNote.getTenantId(),
            itemNote.getItemNoteTypeId());
        } catch (NotFoundException e) {
          return "";
        }
      })
      .collect(Collectors.joining(ITEM_DELIMITER));
  }

  private ItemNote restoreItemNote(String s) {
    if (isNotEmpty(s)) {
      var tokens = s.split(ARRAY_DELIMITER, -1);
      if (tokens.length < NUMBER_OF_ITEM_NOTE_COMPONENTS) {
        throw new EntityFormatException(String.format("Illegal number of item note elements: %d, expected: %d", tokens.length, NUMBER_OF_ITEM_NOTE_COMPONENTS));
      }

      return ItemNote.builder()
        .itemNoteTypeId(ItemReferenceHelper.service().getNoteTypeIdByName(restore(tokens[NOTE_TYPE_NAME_INDEX])))
        .note(Arrays.stream(tokens, NOTE_INDEX, tokens.length - STAFF_ONLY_OFFSET)
          .map(SpecialCharacterEscaper::restore)
          .collect(Collectors.joining(";")))
        .staffOnly(Boolean.valueOf(tokens[tokens.length - STAFF_ONLY_OFFSET]))
        .tenantId(tokens[TENANT_INDEX])
        .build();
    }
    return null;
  }
}
