package org.folio.bulkops.util;

import org.apache.commons.lang3.StringUtils;

import java.util.Optional;

public class Utils {
  public static Optional<String> ofEmptyString(String string) {
    return StringUtils.isNotEmpty(string) ? Optional.of(string) : Optional.empty();
  }
}
