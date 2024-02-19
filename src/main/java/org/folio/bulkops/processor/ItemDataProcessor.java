package org.folio.bulkops.processor;

import static java.lang.String.format;
import static java.util.Objects.isNull;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.folio.bulkops.domain.dto.UpdateActionType.CLEAR_FIELD;
import static org.folio.bulkops.domain.dto.UpdateActionType.REPLACE_WITH;
import static org.folio.bulkops.domain.dto.UpdateActionType.SET_TO_FALSE;
import static org.folio.bulkops.domain.dto.UpdateActionType.SET_TO_TRUE;
import static org.folio.bulkops.domain.dto.UpdateOptionType.PERMANENT_LOAN_TYPE;
import static org.folio.bulkops.domain.dto.UpdateOptionType.STATUS;
import static org.folio.bulkops.domain.dto.UpdateOptionType.SUPPRESS_FROM_DISCOVERY;

import java.util.ArrayList;
import java.util.Date;
import java.util.Objects;

import org.folio.bulkops.domain.bean.InventoryItemStatus;
import org.folio.bulkops.domain.bean.Item;
import org.folio.bulkops.domain.bean.ItemLocation;
import org.folio.bulkops.domain.dto.Action;
import org.folio.bulkops.domain.dto.UpdateOptionType;
import org.folio.bulkops.exception.BulkOperationException;
import org.folio.bulkops.exception.RuleValidationException;
import org.folio.bulkops.service.HoldingsReferenceService;
import org.folio.bulkops.service.ItemReferenceService;
import org.springframework.stereotype.Component;

import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Log4j2
@Component
@AllArgsConstructor
public class ItemDataProcessor extends AbstractDataProcessor<Item> {
  public static final String HOLDINGS_LOCATION_CALL_NUMBER_DELIMITER = " > ";

  private final HoldingsReferenceService holdingsReferenceService;
  private final ItemReferenceService itemReferenceService;
  private final ItemsNotesUpdater itemsNotesUpdater;

  @Override
  public Validator<UpdateOptionType, Action> validator(Item item) {
    return (option, action) -> {
      if (CLEAR_FIELD == action.getType() && STATUS == option) {
        throw new RuleValidationException("Status field can not be cleared");
      } else if (CLEAR_FIELD == action.getType() && PERMANENT_LOAN_TYPE == option) {
        throw new RuleValidationException("Permanent loan type cannot be cleared");
      } else if (CLEAR_FIELD == action.getType() && SUPPRESS_FROM_DISCOVERY == option) {
        throw new RuleValidationException("Suppress from discovery flag cannot be cleared");
      } else if (REPLACE_WITH == action.getType() && isEmpty(action.getUpdated())) {
        throw new RuleValidationException("Loan type value cannot be empty for REPLACE_WITH option");
      } else if (REPLACE_WITH == action.getType() && option == STATUS && !item.getStatus()
        .getName()
        .getValue()
        .equals(action.getUpdated())
          && !itemReferenceService.getAllowedStatuses(item.getStatus()
            .getName()
            .getValue()).contains(action.getUpdated())) {
        throw new RuleValidationException(
            format("New status value \"%s\" is not allowed", action.getUpdated()));
      }
    };
  }

  @Override
  public Updater<Item> updater(UpdateOptionType option, Action action) {
    if (REPLACE_WITH == action.getType()) {
      return switch (option) {
        case PERMANENT_LOAN_TYPE ->
          item -> item.setPermanentLoanType(itemReferenceService.getLoanTypeById(action.getUpdated()));
        case TEMPORARY_LOAN_TYPE ->
          item -> item.setTemporaryLoanType(itemReferenceService.getLoanTypeById(action.getUpdated()));
        case PERMANENT_LOCATION -> item -> {
          item.setPermanentLocation(itemReferenceService.getLocationById(action.getUpdated()));
          item.setEffectiveLocation(getEffectiveLocation(item));
          setupHoldingsData(item);
        };
        case TEMPORARY_LOCATION -> item -> {
          item.setTemporaryLocation(itemReferenceService.getLocationById(action.getUpdated()));
          item.setEffectiveLocation(getEffectiveLocation(item));
          setupHoldingsData(item);
        };
        case STATUS -> item -> item.setStatus(new InventoryItemStatus()
          .withName(InventoryItemStatus.NameEnum.fromValue(action.getUpdated()))
          .withDate(new Date()));
        default -> item -> {
          throw new BulkOperationException(format("Combination %s and %s isn't supported yet", option, action.getType()));
        };
      };
    } else if (SET_TO_TRUE == action.getType()) {
      if (option == SUPPRESS_FROM_DISCOVERY) return item -> item.setDiscoverySuppress(true);
    } else if (SET_TO_FALSE == action.getType()) {
      if (option == SUPPRESS_FROM_DISCOVERY) return item -> item.setDiscoverySuppress(false);
    } else if (CLEAR_FIELD == action.getType()) {
      return switch (option) {
        case PERMANENT_LOCATION -> item -> {
          item.setPermanentLocation(null);
          item.setEffectiveLocation(getEffectiveLocation(item));
          setupHoldingsData(item);
        };
        case TEMPORARY_LOCATION -> item -> {
          item.setTemporaryLocation(null);
          item.setEffectiveLocation(getEffectiveLocation(item));
          setupHoldingsData(item);
        };
        case TEMPORARY_LOAN_TYPE -> item -> item.setTemporaryLoanType(null);
        default -> item -> {
        };
      };
    }
    var notesUpdaterOptional = itemsNotesUpdater.updateNotes(action, option);
    if (notesUpdaterOptional.isPresent()) return notesUpdaterOptional.get();
    return item -> {
      throw new BulkOperationException(format("Combination %s and %s isn't supported yet", option, action.getType()));
    };
  }

  private void setupHoldingsData(Item item) {
    item.setHoldingsData(holdingsReferenceService.getEffectiveLocationCallNumberComponentsForItem(item));
  }

  @Override
  public Item clone(Item entity) {
    var clone = entity.toBuilder()
      .build();
    if (entity.getAdministrativeNotes() != null) {
      var administrativeNotes = new ArrayList<>(entity.getAdministrativeNotes());
      clone.setAdministrativeNotes(administrativeNotes);
    }
    if (entity.getCirculationNotes() != null) {
      var circNotes = entity.getCirculationNotes().stream().map(circulationNote -> circulationNote.toBuilder().build()).toList();
      clone.setCirculationNotes(new ArrayList<>(circNotes));
    }
    if (entity.getNotes() != null) {
      var itemNotes = entity.getNotes().stream().map(itemNote -> itemNote.toBuilder().build()).toList();
      clone.setNotes(new ArrayList<>(itemNotes));
    }
    return clone;
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
      var holdingsRecord = holdingsReferenceService.getHoldingsRecordById(item.getHoldingsRecordId());
      var holdingsEffectiveLocationId = isNull(holdingsRecord.getTemporaryLocationId()) ? holdingsRecord.getPermanentLocationId() : holdingsRecord.getTemporaryLocationId();
      return itemReferenceService.getLocationById(holdingsEffectiveLocationId);
    } else {
      return isNull(item.getTemporaryLocation()) ? item.getPermanentLocation() : item.getTemporaryLocation();
    }
  }
}

