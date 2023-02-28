package org.folio.bulkops.domain.converter;

import static java.util.Objects.isNull;
import static org.apache.commons.lang3.StringUtils.EMPTY;

import org.apache.commons.lang3.ObjectUtils;
import org.folio.bulkops.domain.bean.InventoryItemStatus;
import org.folio.bulkops.exception.EntityFormatException;

import com.opencsv.bean.AbstractBeanField;
import com.opencsv.exceptions.CsvConstraintViolationException;
import com.opencsv.exceptions.CsvDataTypeMismatchException;

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
      return isNull(status.getName()) ? EMPTY : status.getName().getValue();
    }
    return EMPTY;
  }
}
