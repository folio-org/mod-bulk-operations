package org.folio.bulkops.processor;

import static java.lang.String.format;
import static org.apache.commons.lang3.ObjectUtils.isEmpty;
import static org.folio.bulkops.domain.dto.UpdateActionType.CLEAR_FIELD;
import static org.folio.bulkops.domain.dto.UpdateActionType.REPLACE_WITH;
import static org.folio.bulkops.domain.dto.UpdateOptionType.PERMANENT_LOCATION;
import static org.folio.bulkops.domain.dto.UpdateOptionType.TEMPORARY_LOCATION;

import java.util.Objects;
import java.util.regex.Pattern;

import org.folio.bulkops.client.HoldingsSourceClient;
import org.folio.bulkops.client.LocationClient;
import org.folio.bulkops.domain.bean.HoldingsRecord;
import org.folio.bulkops.domain.dto.Action;
import org.folio.bulkops.domain.dto.UpdateOptionType;
import org.folio.bulkops.exception.BulkOperationException;
import org.folio.bulkops.exception.NotFoundException;
import org.folio.bulkops.exception.RuleValidationException;
import org.springframework.stereotype.Component;

import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Log4j2
@Component
@AllArgsConstructor
public class HoldingsDataProcessor extends AbstractDataProcessor<HoldingsRecord> {

  private final LocationClient locationClient;
  private final HoldingsSourceClient holdingsSourceClient;

  private static final Pattern UUID_REGEX =
    Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");

  @Override
  public Validator<UpdateOptionType, Action> validator(HoldingsRecord entity) {
    return (option, action) -> {
      try {
        if ("MARC".equals(holdingsSourceClient.getById(entity.getSourceId()).getName())) {
          throw new RuleValidationException("Holdings records that have source \"MARC\" cannot be changed");
        }
      } catch (NotFoundException e) {
        log.error("Holdings source was not found by id={}", entity.getSourceId());
      }
      if (REPLACE_WITH == action.getType()) {
        var locationId = action.getUpdated();
        if (isEmpty(locationId)) {
          throw new RuleValidationException("Location id cannot be empty");
        }
        if (!UUID_REGEX.matcher(locationId).matches()) {
          throw new RuleValidationException("Location id has invalid format: %s" + locationId);
        }
        try {
          locationClient.getLocationById(locationId);
        } catch (Exception e) {
          throw new RuleValidationException(format("Location %s doesn't exist", locationId));
        }
      }
      if (PERMANENT_LOCATION == option && CLEAR_FIELD == action.getType()) {
        throw new RuleValidationException("Permanent location cannot be cleared");
      }
    };
  }

  public Updater<HoldingsRecord> updater(UpdateOptionType option, Action action) {
    if (PERMANENT_LOCATION != option && TEMPORARY_LOCATION != option) {
      return holding -> {
        throw new BulkOperationException(format("Combination %s and %s isn't supported yet", option, action.getType()));
      };
    }
    switch (action.getType()) {
      case REPLACE_WITH:
        return holding -> {
          var locationId = action.getUpdated();
          if (PERMANENT_LOCATION == option) {
            holding.setPermanentLocation(locationClient.getLocationById(locationId));
            holding.setPermanentLocationId(locationId);
            holding.setEffectiveLocationId(isEmpty(holding.getTemporaryLocationId()) ? locationId : holding.getTemporaryLocationId());
          } else {
            holding.setTemporaryLocationId(locationId);
            holding.setEffectiveLocationId(locationId);
          }
        };
      case CLEAR_FIELD:
        return holding -> {
          holding
            .setTemporaryLocationId(null);
          holding.setEffectiveLocationId(holding
            .getPermanentLocation()
            .getId());
        };
      default:
        return holding -> {
          throw new BulkOperationException(format("Combination %s and %s isn't supported yet", option, action.getType()));
        };
    }
  }

  @Override
  public HoldingsRecord clone(HoldingsRecord entity) {
    return entity.toBuilder().build();
  }

  @Override
  public boolean compare(HoldingsRecord first, HoldingsRecord second) {
    return Objects.equals(first, second);
  }

  @Override
  public Class<HoldingsRecord> getProcessedType() {
    return HoldingsRecord.class;
  }
}
