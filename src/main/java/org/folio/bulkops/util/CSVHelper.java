package org.folio.bulkops.util;

import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import lombok.Getter;
import lombok.experimental.UtilityClass;
import lombok.extern.log4j.Log4j2;

@UtilityClass
@Log4j2
public class CSVHelper {
  private static final char ASCII_ZERO_CHAR = '\0';
  @Getter
  private static final CSVParser csvParser;

  static {
    csvParser = new CSVParserBuilder().withEscapeChar(ASCII_ZERO_CHAR).build();
  }
}
