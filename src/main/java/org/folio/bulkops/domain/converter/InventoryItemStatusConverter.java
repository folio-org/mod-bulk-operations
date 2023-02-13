package org.folio.bulkops.domain.converter;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.folio.bulkops.adapters.BulkEditAdapterHelper.dateFromString;
import static org.folio.bulkops.adapters.BulkEditAdapterHelper.dateToString;
import static org.folio.bulkops.adapters.Constants.ARRAY_DELIMITER;

import com.opencsv.bean.AbstractBeanField;
import com.opencsv.exceptions.CsvConstraintViolationException;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import org.folio.bulkops.domain.bean.InventoryItemStatus;
import org.folio.bulkops.exception.EntityFormatException;

public class InventoryItemStatusConverter extends AbstractBeanField<String, InventoryItemStatus> {
  private static final int NUMBER_OF_STATUS_COMPONENTS = 2;
  private static final int STATUS_NAME_INDEX = 0;
  private static final int STATUS_DATE_INDEX = 1;

  @Override
  protected Object convert(String value) throws CsvDataTypeMismatchException, CsvConstraintViolationException {
    if (isNotEmpty(value)) {
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
    var status = (InventoryItemStatus) value;
    return String.join(ARRAY_DELIMITER, status.getName().getValue(), dateToString(status.getDate()));
  }
}
