package org.folio.bulkops.domain.converter;

import static org.folio.bulkops.domain.format.SpecialCharacterEscaper.escape;
import static org.folio.bulkops.util.Constants.ARRAY_DELIMITER;
import static org.folio.bulkops.util.Constants.ITEM_DELIMITER;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.folio.bulkops.domain.bean.HoldingsStatement;

public class HoldingsStatementListConverter extends BaseConverter<List<HoldingsStatement>> {

  @Override
  public String convertToString(List<HoldingsStatement> object) {
    return object.stream()
      .filter(Objects::nonNull)
      .map(statement ->
        String.join(ARRAY_DELIMITER,
          escape(statement.getStatement()),
          escape(statement.getNote()),
          escape(statement.getStaffNote())))
      .collect(Collectors.joining(ITEM_DELIMITER));
  }
}
