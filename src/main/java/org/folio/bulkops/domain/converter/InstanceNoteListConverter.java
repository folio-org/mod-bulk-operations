package org.folio.bulkops.domain.converter;

import static org.folio.bulkops.domain.format.SpecialCharacterEscaper.escape;
import static org.folio.bulkops.util.Constants.ARRAY_DELIMITER;
import static org.folio.bulkops.util.Constants.ITEM_DELIMITER;
import static org.folio.bulkops.util.Utils.booleanToStringNullSafe;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.folio.bulkops.domain.bean.InstanceNote;
import org.folio.bulkops.service.InstanceReferenceHelper;

public class InstanceNoteListConverter extends BaseConverter<List<InstanceNote>> {

  @Override
  public String convertToString(List<InstanceNote> object) {
    return object.stream()
        .filter(Objects::nonNull)
        .map(
            note ->
                String.join(
                    ARRAY_DELIMITER,
                    escape(
                        InstanceReferenceHelper.service()
                            .getNoteTypeNameById(note.getInstanceNoteTypeId())),
                    escape(note.getNote()),
                    booleanToStringNullSafe(note.getStaffOnly())))
        .collect(Collectors.joining(ITEM_DELIMITER));
  }
}
