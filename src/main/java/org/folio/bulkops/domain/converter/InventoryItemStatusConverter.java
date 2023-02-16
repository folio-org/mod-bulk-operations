package org.folio.bulkops.domain.converter;

import com.opencsv.bean.AbstractBeanField;
import com.opencsv.exceptions.CsvConstraintViolationException;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import org.apache.commons.lang3.ObjectUtils;
import org.folio.bulkops.domain.bean.InventoryItemStatus;
import org.folio.bulkops.exception.EntityFormatException;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.folio.bulkops.adapters.BulkEditAdapterHelper.dateFromString;
import static org.folio.bulkops.util.Constants.ARRAY_DELIMITER;
import static org.folio.bulkops.util.Constants.DATE_TIME_PATTERN;
import static org.folio.bulkops.util.Utils.ofEmptyString;


public class InventoryItemStatusConverter extends AbstractBeanField<String, InventoryItemStatus> {
  private static final int NUMBER_OF_STATUS_COMPONENTS = 2;
  private static final int STATUS_NAME_INDEX = 0;
  private static final int STATUS_DATE_INDEX = 1;

  @Override
  protected Object convert(String value) throws CsvDataTypeMismatchException, CsvConstraintViolationException {
    if (ObjectUtils.isNotEmpty(value)) {
      var tokens = value.split(ARRAY_DELIMITER, -1);
      if (NUMBER_OF_STATUS_COMPONENTS == tokens.length) {
        return InventoryItemStatus.builder()
          .name(InventoryItemStatus.NameEnum.fromValue(tokens[STATUS_NAME_INDEX]))
          .date(dateFromString(tokens[STATUS_DATE_INDEX]))
          .build();
      }
      throw new EntityFormatException(String.format("Illegal number of item status elements: %d, expected: %d", tokens.length, NUMBER_OF_STATUS_COMPONENTS));
    }
    return null;
  }

  @Override
  protected String convertToWrite(Object value) {
    if (ObjectUtils.isNotEmpty(value)) {
      List<String> entries = new ArrayList<>();
      var status = (InventoryItemStatus) value;
      ofEmptyString(status.getName().getValue()).ifPresent(entries::add);
      ofNullable(status.getDate()).ifPresent(d -> entries.add(new SimpleDateFormat(DATE_TIME_PATTERN).format(d)));
      return String.join(ARRAY_DELIMITER, entries);
    }
    return EMPTY;
  }
}
