package org.folio.bulkops.domain.converter;

import com.opencsv.bean.AbstractBeanField;
import com.opencsv.exceptions.CsvConstraintViolationException;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import org.folio.bulkops.domain.bean.LastCheckIn;
import org.folio.bulkops.exception.EntityFormatException;
import org.folio.bulkops.service.ItemReferenceService;

import static org.apache.commons.lang3.ObjectUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.folio.bulkops.adapters.Constants.ARRAY_DELIMITER;
import static org.folio.bulkops.domain.format.SpecialCharacterEscaper.escape;
import static org.folio.bulkops.domain.format.SpecialCharacterEscaper.restore;

public class LastCheckInConverter extends AbstractBeanField<String, LastCheckIn> {
  private static final int NUMBER_OF_LAST_CHECK_IN_COMPONENTS = 3;
  private static final int LAST_CHECK_IN_SERVICE_POINT_NAME_INDEX = 0;
  private static final int LAST_CHECK_IN_USERNAME_INDEX = 1;
  private static final int LAST_CHECK_IN_DATE_TIME_INDEX = 2;

  @Override
  protected LastCheckIn convert(String value) throws CsvDataTypeMismatchException, CsvConstraintViolationException {
    if (isNotEmpty(value)) {
      var tokens = value.split(ARRAY_DELIMITER, -1);
      if (NUMBER_OF_LAST_CHECK_IN_COMPONENTS == tokens.length) {
        return LastCheckIn.builder()
          .servicePointId(ItemReferenceService.service().getServicePointIdByName(restore(tokens[LAST_CHECK_IN_SERVICE_POINT_NAME_INDEX])))
          .staffMemberId(ItemReferenceService.service().getUserIdByUserName(restore(tokens[LAST_CHECK_IN_USERNAME_INDEX])))
          .dateTime(isEmpty(tokens[LAST_CHECK_IN_DATE_TIME_INDEX]) ? null : tokens[LAST_CHECK_IN_DATE_TIME_INDEX])
          .build();
      }
      throw new EntityFormatException(String.format("Illegal number of last check in elements: %d, expected: %d", tokens.length, NUMBER_OF_LAST_CHECK_IN_COMPONENTS));
    }
    return null;
  }

  @Override
  protected String convertToWrite(Object value) {
    if (isEmpty(value)) {
      return EMPTY;
    }
    var lastCheckIn = (LastCheckIn) value;
    return String.join(ARRAY_DELIMITER,
      escape(ItemReferenceService.service().getServicePointNameById(lastCheckIn.getServicePointId())),
      escape(ItemReferenceService.service().getUserNameById(lastCheckIn.getStaffMemberId())),
      lastCheckIn.getDateTime());
  }
}
