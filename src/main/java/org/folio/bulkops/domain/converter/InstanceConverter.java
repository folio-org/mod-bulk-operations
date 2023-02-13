package org.folio.bulkops.domain.converter;

import static org.folio.bulkops.util.Constants.ARRAY_DELIMITER;

import com.opencsv.bean.AbstractBeanField;
import com.opencsv.exceptions.CsvConstraintViolationException;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import org.apache.commons.lang3.StringUtils;
import org.folio.bulkops.service.HoldingsReferenceService;

public class InstanceConverter extends AbstractBeanField<String, String> {

  @Override
  protected Object convert(String value) throws CsvDataTypeMismatchException, CsvConstraintViolationException {
    if (StringUtils.isEmpty(value)) {
      return null;
    }
    var tokens = value.split(ARRAY_DELIMITER);
    var instanceId = tokens.length > 1 ? tokens[tokens.length - 1] : value;
    return StringUtils.isEmpty(instanceId) ? null : instanceId;
  }

  @Override
  protected String convertToWrite(Object value) {
    return String.join(ARRAY_DELIMITER, HoldingsReferenceService.service().getInstanceTitleById(value.toString()), value.toString());
  }
}
