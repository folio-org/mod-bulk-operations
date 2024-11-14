package org.folio.bulkops.util;

import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import lombok.Getter;
import lombok.experimental.UtilityClass;

@UtilityClass
public class CSVHelper {
  private static final char ASCII_ZERO_CHAR = '\0';
  @Getter
  private static final CSVParser csvParser;

  static {
    csvParser = new CSVParserBuilder().withEscapeChar(ASCII_ZERO_CHAR).build();
  }
}
