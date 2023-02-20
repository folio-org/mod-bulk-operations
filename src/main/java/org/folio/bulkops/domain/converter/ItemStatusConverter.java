package org.folio.bulkops.domain.converter;

import com.opencsv.bean.AbstractBeanField;
import com.opencsv.exceptions.CsvConstraintViolationException;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import org.apache.commons.lang3.ObjectUtils;
import org.folio.bulkops.domain.bean.InventoryItemStatus;
import org.folio.bulkops.exception.EntityFormatException;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.folio.bulkops.adapters.BulkEditAdapterHelper.dateFromString;
import static org.folio.bulkops.util.Constants.ARRAY_DELIMITER;
import static org.folio.bulkops.util.Constants.DATE_TIME_PATTERN;
import static org.folio.bulkops.util.Utils.ofEmptyString;


public class ItemStatusConverter extends AbstractBeanField<String, InventoryItemStatus> {

  @Override
  protected Object convert(String value) throws CsvDataTypeMismatchException, CsvConstraintViolationException {
    if (ObjectUtils.isNotEmpty(value)) {
      try {
        return InventoryItemStatus.builder()
          .name(InventoryItemStatus.NameEnum.fromValue(value))
          .build();
      } catch (Exception e) {
        throw new EntityFormatException(String.format("Error - Illegal status name: %s", value));
      }
    }
    return new InventoryItemStatus();
  }

  @Override
  protected String convertToWrite(Object value) {
    if (ObjectUtils.isNotEmpty(value)) {
      var status = (InventoryItemStatus) value;
      return status.getName().getValue();
    }
    return EMPTY;
  }
}
