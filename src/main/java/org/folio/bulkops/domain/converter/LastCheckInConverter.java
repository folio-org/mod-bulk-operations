package org.folio.bulkops.domain.converter;

import static org.apache.commons.lang3.ObjectUtils.isEmpty;
import static org.folio.bulkops.domain.format.SpecialCharacterEscaper.escape;
import static org.folio.bulkops.domain.format.SpecialCharacterEscaper.restore;
import static org.folio.bulkops.util.Constants.ARRAY_DELIMITER;

import org.folio.bulkops.domain.bean.LastCheckIn;
import org.folio.bulkops.exception.EntityFormatException;
import org.folio.bulkops.service.ItemReferenceHelper;

public class LastCheckInConverter extends BaseConverter<LastCheckIn> {
  private static final int NUMBER_OF_LAST_CHECK_IN_COMPONENTS = 3;
  private static final int LAST_CHECK_IN_SERVICE_POINT_NAME_INDEX = 0;
  private static final int LAST_CHECK_IN_USERNAME_INDEX = 1;
  private static final int LAST_CHECK_IN_DATE_TIME_INDEX = 2;

  @Override
  public LastCheckIn convertToObject(String value) {
    var tokens = value.split(ARRAY_DELIMITER, -1);
    if (NUMBER_OF_LAST_CHECK_IN_COMPONENTS == tokens.length) {
      return LastCheckIn.builder()
        .servicePointId(ItemReferenceHelper.service().getServicePointByName(restore(tokens[LAST_CHECK_IN_SERVICE_POINT_NAME_INDEX])).getId())
        .staffMemberId(ItemReferenceHelper.service().getUserIdByUserName(restore(tokens[LAST_CHECK_IN_USERNAME_INDEX])))
        .dateTime(isEmpty(tokens[LAST_CHECK_IN_DATE_TIME_INDEX]) ? null : tokens[LAST_CHECK_IN_DATE_TIME_INDEX])
        .build();
    }
    throw new EntityFormatException(String.format("Illegal number of last check in elements: %d, expected: %d", tokens.length, NUMBER_OF_LAST_CHECK_IN_COMPONENTS));
  }

  @Override
  public String convertToString(LastCheckIn object) {
    return String.join(ARRAY_DELIMITER,
      escape(ItemReferenceHelper.service().getServicePointById(object.getServicePointId()).getName()),
      escape(ItemReferenceHelper.service().getUserNameById(object.getStaffMemberId())),
      object.getDateTime());
  }
}
