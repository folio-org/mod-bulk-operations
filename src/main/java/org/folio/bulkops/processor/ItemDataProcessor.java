package org.folio.bulkops.processor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.bulkops.client.ConfigurationClient;
import org.folio.bulkops.client.LoanTypeClient;
import org.folio.bulkops.client.LocationClient;
import org.folio.bulkops.domain.bean.InventoryItemStatus;
import org.folio.bulkops.domain.bean.Item;
import org.folio.bulkops.domain.dto.Action;
import org.folio.bulkops.domain.dto.BulkOperationRuleCollection;
import org.folio.bulkops.domain.dto.UpdateOptionType;
import org.folio.bulkops.exception.ConfigurationException;
import org.folio.bulkops.exception.NotFoundException;
import org.folio.bulkops.exception.RuleValidationException;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.folio.bulkops.domain.dto.UpdateActionType.CLEAR_FIELD;
import static org.folio.bulkops.domain.dto.UpdateActionType.REPLACE_WITH;
import static org.folio.bulkops.domain.dto.UpdateOptionType.PERMANENT_LOAN_TYPE;
import static org.folio.bulkops.domain.dto.UpdateOptionType.STATUS;

@Log4j2
@Component
@AllArgsConstructor
public class ItemDataProcessor extends AbstractDataProcessor<Item> {

  public static final String BULK_EDIT_CONFIGURATIONS_QUERY_TEMPLATE = "module==%s and configName==%s";
  public static final String MODULE_NAME = "BULKEDIT";
  public static final String STATUSES_CONFIG_NAME = "statuses";

  private final LoanTypeClient loanTypeClient;
  private final LocationClient locationClient;
  private final ConfigurationClient configurationClient;
  private final ObjectMapper objectMapper;


  @Override
  public Validator<UpdateOptionType, Action> validator(Item item) {
    return (option, action) -> {
      if (CLEAR_FIELD == action.getType() && STATUS == option) {
        throw new RuleValidationException("Status field can not be cleared");
      } else if(CLEAR_FIELD == action.getType() && PERMANENT_LOAN_TYPE == option) {
        throw new RuleValidationException("Permanent loan type cannot be cleared");
      } else if(REPLACE_WITH == action.getType() && PERMANENT_LOAN_TYPE == option && action.getUpdated() == null) {
        throw new RuleValidationException("Permanent loan type value cannot be empty");
      } else if(REPLACE_WITH == action.getType() && option == STATUS
        && !item.getStatus().getName().getValue().equals(action.getUpdated())
        && !getAllowedStatuses(item.getStatus().getName().getValue()).contains(action.getUpdated())) {
        throw new RuleValidationException("New status value \\\"%s\\\" is not allowed");
      }
    };
  }

  private List<String> getAllowedStatuses(String statusName) {
    var configurations = configurationClient.getConfigurations(String.format(BULK_EDIT_CONFIGURATIONS_QUERY_TEMPLATE, MODULE_NAME, STATUSES_CONFIG_NAME));
    if (configurations.getConfigs().isEmpty()) {
      throw new NotFoundException("Statuses configuration was not found");
    }
    try {
      var statuses = objectMapper
        .readValue(configurations.getConfigs().get(0).getValue(), new TypeReference<HashMap<String, List<String>>>() {});
      return statuses.getOrDefault(statusName, Collections.emptyList());
    } catch (JsonProcessingException e) {
      throw new ConfigurationException(String.format("Error reading configuration, reason: %s", e.getMessage()));
    }
  }

  @Override
  public Updater<Item> updater(UpdateOptionType option, Action action) {
    if (REPLACE_WITH == action.getType()) {
      switch (option) {
        case PERMANENT_LOAN_TYPE:
          return item -> item.withPermanentLoanType(loanTypeClient.getLoanTypeById(action.getUpdated()));
        case TEMPORARY_LOAN_TYPE:
          return item -> item.withTemporaryLoanType(loanTypeClient.getLoanTypeById(action.getUpdated()));
        case PERMANENT_LOCATION:
          return item -> item.setPermanentLocation(locationClient.getLocationById(action.getUpdated()));
        case TEMPORARY_LOCATION:
          return item -> item.setTemporaryLocation(locationClient.getLocationById(action.getUpdated()));
        case STATUS:
          return item -> item.setStatus(new InventoryItemStatus()
            .withDate(new Date())
            .withName(InventoryItemStatus.NameEnum.fromValue(action.getUpdated())));
        default:
          return item -> {};
      }
    } else if (CLEAR_FIELD == action.getType()) {
      switch (option) {
        case PERMANENT_LOCATION:
          return item -> item.setPermanentLocation(null);
        case TEMPORARY_LOCATION:
          return item -> item.setTemporaryLocation(null);
        case TEMPORARY_LOAN_TYPE:
          return item -> item.setTemporaryLoanType(null);
        default:
          return item -> {};
      }
    }
    return item -> {};

  }

  @Override
  public Item clone(Item entity) {
    return entity.toBuilder().build();
  }

  @Override
  public boolean compare(Item first, Item second) {
    return Objects.equals(first, second);
  }
}
