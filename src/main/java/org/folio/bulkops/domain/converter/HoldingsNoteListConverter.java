package org.folio.bulkops.domain.converter;

import static java.util.Objects.isNull;
import static org.folio.bulkops.domain.format.SpecialCharacterEscaper.escape;
import static org.folio.bulkops.domain.format.SpecialCharacterEscaper.restore;
import static org.folio.bulkops.util.Constants.ARRAY_DELIMITER;
import static org.folio.bulkops.util.Constants.ITEM_DELIMITER;
import static org.folio.bulkops.util.Constants.ITEM_DELIMITER_PATTERN;
import static org.folio.bulkops.util.FolioExecutionContextUtil.prepareContextForTenant;
import static org.folio.bulkops.util.Utils.booleanToStringNullSafe;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.ObjectUtils;
import org.folio.bulkops.domain.bean.HoldingsNote;
import org.folio.bulkops.exception.ConverterException;
import org.folio.bulkops.exception.EntityFormatException;
import org.folio.bulkops.exception.NotFoundException;
import org.folio.bulkops.service.HoldingsReferenceHelper;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.FolioModuleMetadata;
import org.folio.spring.scope.FolioExecutionContextSetter;
import org.springframework.stereotype.Component;

@Log4j2
public class HoldingsNoteListConverter extends BaseConverter<List<HoldingsNote>> {
  private static final int NUMBER_OF_HOLDINGS_NOTE_ELEMENTS = 5;
  private static final int HOLDINGS_NOTE_NOTE_TYPE_INDEX = 0;
  private static final int HOLDINGS_NOTE_NOTE_INDEX = 1;
  private static final int TENANT_INDEX = 3;
  private static final int HOLDINGS_NOTE_STAFF_ONLY_INDEX = 2;

  @Override
  public List<HoldingsNote> convertToObject(String value) {
    return Arrays.stream(value.split(ITEM_DELIMITER_PATTERN))
      .map(this::restoreHoldingsNote)
      .filter(ObjectUtils::isNotEmpty)
      .toList();
  }

  @Override
  public String convertToString(List<HoldingsNote> object) {
    return object.stream()
      .filter(Objects::nonNull)
      .map(note -> {
        var noteTypeName = note.getHoldingsNoteTypeName();
        if (isNull(noteTypeName)) {
          noteTypeName = "";
          try {
            noteTypeName = HoldingsReferenceHelper.service().getNoteTypeNameById(note.getHoldingsNoteTypeId());
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
      .tenantId(tokens[TENANT_INDEX])
      .build();
  }
}
