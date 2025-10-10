package org.folio.bulkops.domain.format;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.folio.bulkops.util.Constants.ARRAY_DELIMITER;
import static org.folio.bulkops.util.Constants.ITEM_DELIMITER;
import static org.folio.bulkops.util.Constants.KEY_VALUE_DELIMITER;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;

public class SpecialCharacterEscaper {

  private static final String[] SPECIAL_CHARACTERS = {ITEM_DELIMITER, ARRAY_DELIMITER,
    KEY_VALUE_DELIMITER};
  private static final String[] ESCAPED_VALUES = {"%7C", "%3B", "%3A"};

  public static String escape(String initial) {
    if (StringUtils.isEmpty(initial)) {
      return EMPTY;
    }
    for (int i = 0; i < SPECIAL_CHARACTERS.length; i++) {
      initial = initial.replace(SPECIAL_CHARACTERS[i], ESCAPED_VALUES[i]);
    }
    return initial;
  }

  public static List<String> escape(List<String> initial) {
    if (initial == null) {
      return Collections.emptyList();
    }
    return initial.stream().map(SpecialCharacterEscaper::escape).collect(Collectors.toList());
  }

  public static String restore(String escaped) {
    if (StringUtils.isEmpty(escaped)) {
      return null;
    }
    for (int i = 0; i < ESCAPED_VALUES.length; i++) {
      escaped = escaped.replace(ESCAPED_VALUES[i], SPECIAL_CHARACTERS[i]);
    }
    return escaped;
  }

  public static List<String> restore(List<String> escaped) {
    if (escaped == null) {
      return Collections.emptyList();
    }
    return escaped.stream().map(SpecialCharacterEscaper::restore).collect(Collectors.toList());
  }
}
