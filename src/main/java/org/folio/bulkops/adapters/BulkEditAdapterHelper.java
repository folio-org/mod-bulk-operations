package org.folio.bulkops.adapters;

import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.folio.bulkops.adapters.Constants.DATE_TIME_PATTERN;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.OptionalInt;
import java.util.TimeZone;
import java.util.stream.IntStream;

import org.folio.bulkops.domain.dto.UnifiedTable;
import org.folio.bulkops.error.BulkOperationException;

import lombok.experimental.UtilityClass;

@UtilityClass
public class BulkEditAdapterHelper {
  private static final DateFormat dateFormat;

  static {
    dateFormat = new SimpleDateFormat(DATE_TIME_PATTERN);
    dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
  }

  public static String dateToString(Date date) {
    return nonNull(date) ? dateFormat.format(date) : EMPTY;
  }

  public static String getValueFromTable(String field, UnifiedTable table) {
    OptionalInt index = IntStream.range(0, table.getHeader()
      .size())
      .filter(i -> field.equals(table.getHeader()
        .get(i)
        .getValue()))
      .findFirst();
    if (index.isPresent()) {
      return table.getRows()
        .get(0)
        .getRow()
        .get(index.getAsInt());
    }
    throw new BulkOperationException("Position error for field " + field);
  }
}
