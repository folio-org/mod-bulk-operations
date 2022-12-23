package org.folio.bulkops.processor;

import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.bulkops.client.HoldingsSourceClient;
import org.folio.bulkops.client.LocationClient;
import org.folio.bulkops.domain.bean.HoldingsRecord;
import org.folio.bulkops.domain.dto.Action;
import org.folio.bulkops.domain.dto.UpdateOptionType;
import org.folio.bulkops.exception.RuleValidationException;
import org.springframework.stereotype.Component;

import java.util.Objects;

import static java.util.Objects.isNull;
import static org.apache.commons.lang3.ObjectUtils.isEmpty;
import static org.folio.bulkops.domain.dto.UpdateActionType.REPLACE_WITH;
import static org.folio.bulkops.domain.dto.UpdateOptionType.PERMANENT_LOCATION;
import static org.folio.bulkops.domain.dto.UpdateOptionType.TEMPORARY_LOCATION;

@Log4j2
@Component
@AllArgsConstructor
public class HoldingsDataProcessor extends AbstractDataProcessor<HoldingsRecord> {

  private final LocationClient locationClient;
  private final HoldingsSourceClient holdingsSourceClient;

  @Override
  public Validator<UpdateOptionType, Action> validator(HoldingsRecord entity) {
    return (option, action) -> {
      if ("MARC".equals(holdingsSourceClient.getById(entity.getSourceId()).getName())) {
        throw new RuleValidationException("Holdings records that have source \"MARC\" cannot be changed");
      }
      if (PERMANENT_LOCATION == option) {
        if (REPLACE_WITH == action.getType()) {
          if (isNull(action.getUpdated())) {
            throw new RuleValidationException("Location name cannot be empty");
          }
          try {
            locationClient.getLocationById(action.getUpdated());
          } catch (Exception e) {
            throw new RuleValidationException(String.format("Location %s doesn't exist", entity.getId()));
          }
        } else {
          throw new RuleValidationException("Permanent location cannot be cleared");
        }
      }
    };
  }

  public Updater<HoldingsRecord> updater(UpdateOptionType option, Action action) {
    switch (action.getType()) {
      case REPLACE_WITH:
        return holding -> {
          var locationId = action.getUpdated();
          if (PERMANENT_LOCATION == option) {
            holding.setPermanentLocation(locationClient.getLocationById(locationId));
            holding.setEffectiveLocationId(isEmpty(holding.getTemporaryLocationId()) ? locationId : holding.getTemporaryLocationId());
          } else if (TEMPORARY_LOCATION == option) {
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
        return holding -> {};
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
}
