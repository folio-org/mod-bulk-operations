package org.folio.bulkops.util;

import com.fasterxml.jackson.databind.MappingIterator;
import org.folio.bulkops.domain.bean.BulkOperationsEntity;

import static java.util.Objects.isNull;
import static org.apache.commons.lang3.StringUtils.LF;

public class Utils {

  public static String prepareForCsv(String s) {
    if (isNull(s)) {
      return "";
    }

    if (s.contains("\"")) {
      s = s.replace("\"", "\"\"");
    }

    if (s.contains(LF)) {
      s = s.replace(LF, "\\n");
    }

    if (s.contains(",")) {
      s = "\"" + s + "\"";
    }

    return s;
  }

  public static boolean hasNextRecord(MappingIterator<? extends BulkOperationsEntity> originalFileIterator, MappingIterator<? extends BulkOperationsEntity> modifiedFileIterator) {
    return originalFileIterator.hasNext() && modifiedFileIterator.hasNext();
  }
}
