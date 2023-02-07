package org.folio.bulkops.processor;

import static java.lang.String.format;
import static java.util.Objects.isNull;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.folio.bulkops.domain.dto.UpdateActionType.CLEAR_FIELD;
import static org.folio.bulkops.domain.dto.UpdateActionType.REPLACE_WITH;
import static org.folio.bulkops.domain.dto.UpdateOptionType.PERMANENT_LOAN_TYPE;
import static org.folio.bulkops.domain.dto.UpdateOptionType.STATUS;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import org.folio.bulkops.client.ConfigurationClient;
import org.folio.bulkops.client.HoldingsClient;
import org.folio.bulkops.client.LoanTypeClient;
import org.folio.bulkops.client.LocationClient;
import org.folio.bulkops.domain.bean.InventoryItemStatus;
import org.folio.bulkops.domain.bean.Item;
import org.folio.bulkops.domain.bean.ItemLocation;
import org.folio.bulkops.domain.dto.Action;
import org.folio.bulkops.domain.dto.UpdateOptionType;
import org.folio.bulkops.exception.BulkOperationException;
import org.folio.bulkops.exception.ConfigurationException;
import org.folio.bulkops.exception.NotFoundException;
import org.folio.bulkops.exception.RuleValidationException;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Log4j2
@Component
@AllArgsConstructor
public class ItemDataProcessor extends AbstractDataProcessor<Item> {

  public static final String BULK_EDIT_CONFIGURATIONS_QUERY_TEMPLATE = "module==%s and configName==%s";
  public static final String MODULE_NAME = "BULKEDIT";
  public static final String STATUSES_CONFIG_NAME = "statuses";

  private final LoanTypeClient loanTypeClient;
  private final LocationClient locationClient;
  private final HoldingsClient holdingsClient;
  private final ConfigurationClient configurationClient;
  private final ObjectMapper objectMapper;

  @Override
  public Validator<UpdateOptionType, Action> validator(Item item) {
    return (option, action) -> {
      if (CLEAR_FIELD == action.getType() && STATUS == option) {
        throw new RuleValidationException("Status field can not be cleared");
      } else if (CLEAR_FIELD == action.getType() && PERMANENT_LOAN_TYPE == option) {
        throw new RuleValidationException("Permanent loan type cannot be cleared");
      } else if (REPLACE_WITH == action.getType() && isEmpty(action.getUpdated())) {
        throw new RuleValidationException("Loan type value cannot be empty for REPLACE_WITH option");
      } else if (REPLACE_WITH == action.getType() && option == STATUS && !item.getStatus()
        .getName()
        .getValue()
        .equals(action.getUpdated())
          && !getAllowedStatuses(item.getStatus()
            .getName()
            .getValue()).contains(action.getUpdated())) {
        throw new RuleValidationException(
            format("Actual status value \"%s\" cannot be changed to \"%s\"", item.getStatus(), action.getUpdated()));
      }
    };
  }

  private List<String> getAllowedStatuses(String statusName) {
    var configurations = configurationClient
      .getConfigurations(format(BULK_EDIT_CONFIGURATIONS_QUERY_TEMPLATE, MODULE_NAME, STATUSES_CONFIG_NAME));
    if (configurations.getConfigs()
      .isEmpty()) {
      throw new NotFoundException("Statuses configuration was not found");
    }
    try {
      var statuses = objectMapper.readValue(configurations.getConfigs()
        .get(0)
        .getValue(), new TypeReference<HashMap<String, List<String>>>() {
        });
      return statuses.getOrDefault(statusName, Collections.emptyList());
    } catch (JsonProcessingException e) {
      throw new ConfigurationException(format("Error reading configuration, reason: %s", e.getMessage()));
    }
  }

  @Override
  public Updater<Item> updater(UpdateOptionType option, Action action) {
    if (REPLACE_WITH == action.getType()) {
      switch (option) {
      case PERMANENT_LOAN_TYPE:
        return item -> item.setPermanentLoanType(loanTypeClient.getLoanTypeById(action.getUpdated()));
      case TEMPORARY_LOAN_TYPE:
        return item -> item.setTemporaryLoanType(loanTypeClient.getLoanTypeById(action.getUpdated()));
      case PERMANENT_LOCATION:
        return item -> {
          item.setPermanentLocation(locationClient.getLocationById(action.getUpdated()));
          item.setEffectiveLocation(getEffectiveLocation(item));
        };
      case TEMPORARY_LOCATION:
        return item -> {
          item.setTemporaryLocation(locationClient.getLocationById(action.getUpdated()));
          item.setEffectiveLocation(getEffectiveLocation(item));
        };
      case STATUS:
        return item -> item.setStatus(new InventoryItemStatus().withDate(new Date())
          .withName(InventoryItemStatus.NameEnum.fromValue(action.getUpdated())));
      default:
        return item -> {
          throw new BulkOperationException(format("Combination %s and %s isn't supported yet", option, action.getType()));
        };
      }
    } else if (CLEAR_FIELD == action.getType()) {
      switch (option) {
      case PERMANENT_LOCATION:
        return item -> {
          item.setPermanentLocation(null);
          item.setEffectiveLocation(getEffectiveLocation(item));
        };
      case TEMPORARY_LOCATION:
        return item -> {
          item.setTemporaryLocation(null);
          item.setEffectiveLocation(getEffectiveLocation(item));
        };
      case TEMPORARY_LOAN_TYPE:
        return item -> item.setTemporaryLoanType(null);
      default:
        return item -> {
        };
      }
    }
    return item -> {
      throw new BulkOperationException(format("Combination %s and %s isn't supported yet", option, action.getType()));
    };

  }

  @Override
  public Item clone(Item entity) {
    return entity.toBuilder()
      .build();
  }

  @Override
  public boolean compare(Item first, Item second) {
    return Objects.equals(first, second);
  }

  @Override
  public Class<Item> getProcessedType() {
    return Item.class;
  }

  private ItemLocation getEffectiveLocation(Item item) {
    if (isNull(item.getTemporaryLocation()) && isNull(item.getPermanentLocation())) {
      var holdingsJson = holdingsClient.getHoldingById(item.getHoldingsRecordId());
      var holdingsEffectiveLocationId = isEmpty(holdingsJson.get("temporaryLocationId").asText()) ? holdingsJson.get("permanentLocationId") : holdingsJson.get("temporaryLocationId");
      return locationClient.getLocationById(holdingsEffectiveLocationId.asText());
    } else {
      return isNull(item.getTemporaryLocation()) ? item.getPermanentLocation() : item.getTemporaryLocation();
    }
  }
}
