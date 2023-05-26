package org.folio.bulkops.domain.converter;

import static org.apache.commons.lang3.ObjectUtils.isEmpty;
import static org.folio.bulkops.domain.format.SpecialCharacterEscaper.escape;
import static org.folio.bulkops.domain.format.SpecialCharacterEscaper.restore;
import static org.folio.bulkops.util.Constants.ARRAY_DELIMITER;
import static org.folio.bulkops.util.Constants.ITEM_DELIMITER;
import static org.folio.bulkops.util.Constants.ITEM_DELIMITER_PATTERN;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.folio.bulkops.domain.bean.HoldingsStatement;
import org.folio.bulkops.exception.EntityFormatException;

public class HoldingsStatementListConverter extends BaseConverter<List<HoldingsStatement>> {
  private static final int NUMBER_OF_HOLDINGS_STATEMENT_ELEMENTS = 3;
  private static final int HOLDINGS_STATEMENT_STATEMENT_INDEX = 0;
  private static final int HOLDINGS_STATEMENT_NOTE_INDEX = 1;
  private static final int HOLDINGS_STATEMENT_STAFF_NOTE_INDEX = 2;

  @Override
  public List<HoldingsStatement> convertToObject(String value) {
    return Arrays.stream(value.split(ITEM_DELIMITER_PATTERN))
      .map(this::restoreHoldingsStatement)
      .filter(Objects::nonNull)
      .toList();
  }

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

  private HoldingsStatement restoreHoldingsStatement(String statementString) {
    if (isEmpty(statementString)) {
      return null;
    }
    var tokens = statementString.split(ARRAY_DELIMITER, -1);
    if (tokens.length < NUMBER_OF_HOLDINGS_STATEMENT_ELEMENTS) {
      throw new EntityFormatException(String.format("Illegal number of holdings statement elements: %d, expected: %d", tokens.length, NUMBER_OF_HOLDINGS_STATEMENT_ELEMENTS));
    }
    return HoldingsStatement.builder()
      .statement(restore(tokens[HOLDINGS_STATEMENT_STATEMENT_INDEX]))
      .note(restore(tokens[HOLDINGS_STATEMENT_NOTE_INDEX]))
      .staffNote(restore(tokens[HOLDINGS_STATEMENT_STAFF_NOTE_INDEX]))
      .build();
  }
}
