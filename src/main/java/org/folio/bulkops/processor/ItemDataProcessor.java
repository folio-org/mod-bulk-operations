package org.folio.bulkops.processor;

import static java.lang.String.format;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.folio.bulkops.domain.dto.UpdateActionType.CLEAR_FIELD;
import static org.folio.bulkops.domain.dto.UpdateActionType.REPLACE_WITH;
import static org.folio.bulkops.domain.dto.UpdateActionType.SET_TO_FALSE;
import static org.folio.bulkops.domain.dto.UpdateActionType.SET_TO_TRUE;
import static org.folio.bulkops.domain.dto.UpdateOptionType.ELECTRONIC_ACCESS_URL_RELATIONSHIP;
import static org.folio.bulkops.domain.dto.UpdateOptionType.PERMANENT_LOAN_TYPE;
import static org.folio.bulkops.domain.dto.UpdateOptionType.STATUS;
import static org.folio.bulkops.domain.dto.UpdateOptionType.SUPPRESS_FROM_DISCOVERY;
import static org.folio.bulkops.util.Constants.RECORD_CANNOT_BE_UPDATED_ERROR_TEMPLATE;

import java.util.ArrayList;
import java.util.Date;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.folio.bulkops.domain.bean.ExtendedItem;
import org.folio.bulkops.domain.bean.InventoryItemStatus;
import org.folio.bulkops.domain.bean.Item;
import org.folio.bulkops.domain.bean.ItemLocation;
import org.folio.bulkops.domain.dto.Action;
import org.folio.bulkops.domain.dto.UpdateOptionType;
import org.folio.bulkops.domain.dto.BulkOperationRule;
import org.folio.bulkops.exception.BulkOperationException;
import org.folio.bulkops.exception.RuleValidationException;
import org.folio.bulkops.exception.RuleValidationTenantsException;
import org.folio.bulkops.service.HoldingsReferenceService;
import org.folio.bulkops.service.ItemReferenceService;
import org.springframework.stereotype.Component;

import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Log4j2
@Component
@AllArgsConstructor
public class ItemDataProcessor extends AbstractDataProcessor<ExtendedItem> {
  private final HoldingsReferenceService holdingsReferenceService;
  private final ItemReferenceService itemReferenceService;
  private final ItemsNotesUpdater itemsNotesUpdater;

  @Override
  public Validator<UpdateOptionType, Action, BulkOperationRule> validator(ExtendedItem extendedItem) {
    return (option, action, rule) -> {
      if (CLEAR_FIELD == action.getType() && STATUS == option) {
        throw new RuleValidationException("Status field can not be cleared");
      } else if (CLEAR_FIELD == action.getType() && PERMANENT_LOAN_TYPE == option) {
        throw new RuleValidationException("Permanent loan type cannot be cleared");
      } else if (CLEAR_FIELD == action.getType() && SUPPRESS_FROM_DISCOVERY == option) {
        throw new RuleValidationException("Suppress from discovery flag cannot be cleared");
      } else if (REPLACE_WITH == action.getType() && isEmpty(action.getUpdated())) {
        throw new RuleValidationException("Loan type value cannot be empty for REPLACE_WITH option");
      } else if (REPLACE_WITH == action.getType() && option == STATUS && !extendedItem.getEntity().getStatus()
        .getName()
        .getValue()
        .equals(action.getUpdated())
          && !itemReferenceService.getAllowedStatuses(extendedItem.getEntity().getStatus()
            .getName()
            .getValue()).contains(action.getUpdated())) {
        throw new RuleValidationException(
            format("New status value \"%s\" is not allowed", action.getUpdated()));
      }
//      if (ruleTenantsAreNotValid(rule, action, option, extendedItem)) {
//        throw new RuleValidationTenantsException(String.format(RECORD_CANNOT_BE_UPDATED_ERROR_TEMPLATE, extendedItem.getIdentifier(org.folio.bulkops.domain.dto.IdentifierType.ID), extendedItem.getTenant(), option.getValue()));
//      }
    };
  }

  @Override
  public Updater<ExtendedItem> updater(UpdateOptionType option, Action action, ExtendedItem entity, BulkOperationRule rule) throws RuleValidationTenantsException {
    if (REPLACE_WITH == action.getType()) {
      return switch (option) {
        case PERMANENT_LOAN_TYPE ->
          extendedItem -> extendedItem.getEntity().setPermanentLoanType(itemReferenceService.getLoanTypeById(action.getUpdated()));
        case TEMPORARY_LOAN_TYPE ->
          extendedItem -> extendedItem.getEntity().setTemporaryLoanType(itemReferenceService.getLoanTypeById(action.getUpdated()));
        case PERMANENT_LOCATION -> extendedItem -> {
          extendedItem.getEntity().setPermanentLocation(itemReferenceService.getLocationById(action.getUpdated()));
          extendedItem.getEntity().setEffectiveLocation(getEffectiveLocation(extendedItem.getEntity()));
        };
        case TEMPORARY_LOCATION -> extendedItem -> {
          extendedItem.getEntity().setTemporaryLocation(itemReferenceService.getLocationById(action.getUpdated()));
          extendedItem.getEntity().setEffectiveLocation(getEffectiveLocation(extendedItem.getEntity()));
        };
        case STATUS -> extendedItem -> extendedItem.getEntity().setStatus(new InventoryItemStatus()
          .withName(InventoryItemStatus.NameEnum.fromValue(action.getUpdated()))
          .withDate(new Date()));
        default -> item -> {
          throw new BulkOperationException(format("Combination %s and %s isn't supported yet", option, action.getType()));
        };
      };
    } else if (SET_TO_TRUE == action.getType()) {
      if (option == SUPPRESS_FROM_DISCOVERY) return extendedItem -> extendedItem.getEntity().setDiscoverySuppress(true);
    } else if (SET_TO_FALSE == action.getType()) {
      if (option == SUPPRESS_FROM_DISCOVERY) return extendedItem -> extendedItem.getEntity().setDiscoverySuppress(false);
    } else if (CLEAR_FIELD == action.getType()) {
      return switch (option) {
        case PERMANENT_LOCATION -> extendedItem -> {
          extendedItem.getEntity().setPermanentLocation(null);
          extendedItem.getEntity().setEffectiveLocation(getEffectiveLocation(extendedItem.getEntity()));
        };
        case TEMPORARY_LOCATION -> extendedItem -> {
          extendedItem.getEntity().setTemporaryLocation(null);
          extendedItem.getEntity().setEffectiveLocation(getEffectiveLocation(extendedItem.getEntity()));
        };
        case TEMPORARY_LOAN_TYPE -> extendedItem -> extendedItem.getEntity().setTemporaryLoanType(null);
        default -> item -> {
        };
      };
    }
    var notesUpdaterOptional = isNull(rule) ? itemsNotesUpdater.updateNotes(action, option) : itemsNotesUpdater.updateNotes(action, option, rule, entity);
    if (notesUpdaterOptional.isPresent()) return notesUpdaterOptional.get();
    return item -> {
      throw new BulkOperationException(format("Combination %s and %s isn't supported yet", option, action.getType()));
    };
  }

  @Override
  public ExtendedItem clone(ExtendedItem extendedItem) {
    var entity = extendedItem.getEntity();
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
    return ExtendedItem.builder().tenantId(extendedItem.getTenantId()).entity(clone).build();
  }

  @Override
  public boolean compare(ExtendedItem first, ExtendedItem second) {
    if (StringUtils.equals(first.getTenantId(), second.getTenantId())) {
      return Objects.equals(first.getEntity(), second.getEntity());
    }
    return false;
  }

  @Override
  public Class<ExtendedItem> getProcessedType() {
    return ExtendedItem.class;
  }

  private ItemLocation getEffectiveLocation(Item item) {
    if (isNull(item.getTemporaryLocation()) && isNull(item.getPermanentLocation())) {
      var holdingsRecord = holdingsReferenceService.getHoldingsRecordById(item.getHoldingsRecordId(), folioExecutionContext.getTenantId());
      var holdingsEffectiveLocationId = isNull(holdingsRecord.getTemporaryLocationId()) ? holdingsRecord.getPermanentLocationId() : holdingsRecord.getTemporaryLocationId();
      return itemReferenceService.getLocationById(holdingsEffectiveLocationId);
    } else {
      return isNull(item.getTemporaryLocation()) ? item.getPermanentLocation() : item.getTemporaryLocation();
    }
  }

//  private boolean ruleTenantsAreNotValid(BulkOperationRule rule, Action action, UpdateOptionType option, ExtendedItem extendedItem) {
//    var ruleTenants = rule.getRuleDetails().getTenants();
//    var actionTenants = action.getTenants();
//    if (nonNull(ruleTenants) && !ruleTenants.isEmpty() && nonNull(actionTenants) && !actionTenants.isEmpty()) {
//      ruleTenants.retainAll(actionTenants);
//      return !ruleTenants.contains(extendedItem.getTenant());
//    }
//    if (nonNull(ruleTenants) && nonNull(actionTenants) && ruleTenants.isEmpty() && actionTenants.isEmpty() &&
//      option == ELECTRONIC_ACCESS_URL_RELATIONSHIP && action.getType() == org.folio.bulkops.domain.dto.UpdateActionType.FIND_AND_REPLACE) {
//      return true;
//    }
//    return nonNull(ruleTenants) && !ruleTenants.isEmpty() && !ruleTenants.contains(extendedItem.getTenant()) ||
//      nonNull(actionTenants) && !actionTenants.isEmpty() && !actionTenants.contains(extendedItem.getTenant());
//  }
}

