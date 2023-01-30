package org.folio.bulkops.util;

import java.time.ZoneId;

public class Constants {

  public static final String COMMA_DELIMETER = ",";
  public static final String NEW_LINE_SEPARATOR = "\n";

  private Constants() {
  }

  public static final String UTC = "UTC";
  public static final ZoneId UTC_ZONE = ZoneId.of(UTC);
}
