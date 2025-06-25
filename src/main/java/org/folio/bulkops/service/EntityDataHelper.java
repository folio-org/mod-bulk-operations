package org.folio.bulkops.service;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.ObjectUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.SPACE;
import static org.folio.bulkops.util.Constants.CALL_NUMBER;
import static org.folio.bulkops.util.Constants.CALL_NUMBER_PREFIX;
import static org.folio.bulkops.util.Constants.CALL_NUMBER_SUFFIX;
import static org.folio.bulkops.util.Constants.HOLDINGS_LOCATION_CALL_NUMBER_DELIMITER;
import static org.folio.bulkops.util.Constants.INACTIVE;
import static org.folio.bulkops.util.Constants.IS_ACTIVE;
import static org.folio.bulkops.util.Constants.NAME;
import static org.folio.bulkops.util.Constants.PERMANENT_LOCATION_ID;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
@RequiredArgsConstructor
public class EntityDataHelper {
  private final HoldingsReferenceService holdingsReferenceService;

  public String getInstanceTitle(String holdingsRecordId, String tenantId) {
    return ofNullable(holdingsReferenceService.getHoldingsRecordById(holdingsRecordId, tenantId))
      .map(holdingsRecord -> holdingsReferenceService.getInstanceTitleById(holdingsRecord.getInstanceId(), tenantId))
      .orElse(EMPTY);
  }

  public String getHoldingsData(String holdingsId, String tenantId) {
    if (isEmpty(holdingsId)) {
      return EMPTY;
    }
    var holdingsJson = holdingsReferenceService.getHoldingsJsonById(holdingsId, tenantId);
    var locationId = isNull(holdingsJson.get(PERMANENT_LOCATION_ID)) ? null : holdingsJson.get(PERMANENT_LOCATION_ID).asText();

    var locationJson = holdingsReferenceService.getHoldingsLocationById(locationId, tenantId);
    var activePrefix = nonNull(locationJson.get(IS_ACTIVE)) && locationJson.get(IS_ACTIVE).asBoolean() ? EMPTY : INACTIVE;
    var name = isNull(locationJson.get(NAME)) ? EMPTY : locationJson.get(NAME).asText();
    var locationName = activePrefix + name;

    var callNumber = Stream.of(holdingsJson.get(CALL_NUMBER_PREFIX), holdingsJson.get(CALL_NUMBER), holdingsJson.get(CALL_NUMBER_SUFFIX))
      .filter(Objects::nonNull)
      .map(JsonNode::asText)
      .collect(Collectors.joining(SPACE));

    return String.join(HOLDINGS_LOCATION_CALL_NUMBER_DELIMITER, locationName, callNumber);
  }
}
