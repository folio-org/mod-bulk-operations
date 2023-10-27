package org.folio.bulkops.processor;

import static java.lang.String.format;
import static org.apache.commons.lang3.ObjectUtils.isEmpty;
import static org.folio.bulkops.domain.dto.UpdateActionType.ADD_TO_EXISTING;
import static org.folio.bulkops.domain.dto.UpdateActionType.CHANGE_TYPE;
import static org.folio.bulkops.domain.dto.UpdateActionType.CLEAR_FIELD;
import static org.folio.bulkops.domain.dto.UpdateActionType.FIND_AND_REMOVE_THESE;
import static org.folio.bulkops.domain.dto.UpdateActionType.FIND_AND_REPLACE;
import static org.folio.bulkops.domain.dto.UpdateActionType.MARK_AS_STAFF_ONLY;
import static org.folio.bulkops.domain.dto.UpdateActionType.REMOVE_ALL;
import static org.folio.bulkops.domain.dto.UpdateActionType.REMOVE_MARK_AS_STAFF_ONLY;
import static org.folio.bulkops.domain.dto.UpdateActionType.REPLACE_WITH;
import static org.folio.bulkops.domain.dto.UpdateActionType.SET_TO_FALSE;
import static org.folio.bulkops.domain.dto.UpdateActionType.SET_TO_FALSE_INCLUDING_ITEMS;
import static org.folio.bulkops.domain.dto.UpdateActionType.SET_TO_TRUE;
import static org.folio.bulkops.domain.dto.UpdateActionType.SET_TO_TRUE_INCLUDING_ITEMS;
import static org.folio.bulkops.domain.dto.UpdateOptionType.ADMINISTRATIVE_NOTE;
import static org.folio.bulkops.domain.dto.UpdateOptionType.HOLDINGS_NOTE;
import static org.folio.bulkops.domain.dto.UpdateOptionType.PERMANENT_LOCATION;
import static org.folio.bulkops.domain.dto.UpdateOptionType.SUPPRESS_FROM_DISCOVERY;

import java.util.ArrayList;
import java.util.Objects;
import java.util.regex.Pattern;

import org.folio.bulkops.domain.bean.HoldingsRecord;
import org.folio.bulkops.domain.dto.Action;
import org.folio.bulkops.domain.dto.UpdateActionType;
import org.folio.bulkops.domain.dto.UpdateOptionType;
import org.folio.bulkops.exception.BulkOperationException;
import org.folio.bulkops.exception.NotFoundException;
import org.folio.bulkops.exception.RuleValidationException;
import org.folio.bulkops.service.HoldingsReferenceService;
import org.folio.bulkops.service.ItemReferenceService;
import org.springframework.stereotype.Component;

import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Log4j2
@Component
@AllArgsConstructor
public class HoldingsDataProcessor extends AbstractDataProcessor<HoldingsRecord> {


  private final ItemReferenceService itemReferenceService;
  private final HoldingsReferenceService holdingsReferenceService;
  private final HoldingsNotesUpdater holdingsNotesUpdater;

  private static final Pattern UUID_REGEX =
    Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");

  @Override
  public Validator<UpdateOptionType, Action> validator(HoldingsRecord entity) {
    return (option, action) -> {
      try {
        if ("MARC".equals(holdingsReferenceService.getSourceById(entity.getSourceId()).getName())) {
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
          itemReferenceService.getLocationById(locationId);
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
    if (REPLACE_WITH == action.getType()) {
      return holding -> {
        var locationId = action.getUpdated();
        if (PERMANENT_LOCATION == option) {
          holding.setPermanentLocation(itemReferenceService.getLocationById(locationId));
          holding.setPermanentLocationId(locationId);
          holding.setEffectiveLocationId(isEmpty(holding.getTemporaryLocationId()) ? locationId : holding.getTemporaryLocationId());
        } else {
          holding.setTemporaryLocationId(locationId);
          holding.setEffectiveLocationId(locationId);
        }
      };
    } else if (CLEAR_FIELD == action.getType()) {
      return holding -> {
        holding.setTemporaryLocationId(null);
        holding.setEffectiveLocationId(holding.getPermanentLocationId());
      };
    } else if (isSetDiscoverySuppressTrue(action.getType(), option)) {
      return holding -> holding.setDiscoverySuppress(true);
    } else if (isSetDiscoverySuppressFalse(action.getType(), option)) {
      return holding -> holding.setDiscoverySuppress(false);
    } else if (MARK_AS_STAFF_ONLY == action.getType() || REMOVE_MARK_AS_STAFF_ONLY == action.getType()){
      var markAsStaffValue = action.getType() == MARK_AS_STAFF_ONLY;
      return holding -> holdingsNotesUpdater.setMarkAsStaffForNotesByTypeId(holding.getNotes(), action.getParameters(), markAsStaffValue);
    } else if (REMOVE_ALL == action.getType()) {
      if (option == ADMINISTRATIVE_NOTE) {
        return holding -> holding.setAdministrativeNotes(holdingsNotesUpdater.removeAdministrativeNotes());
      } else if (option == HOLDINGS_NOTE) {
        return holding -> holding.setNotes(holdingsNotesUpdater.removeNotesByTypeId(holding.getNotes(), action.getParameters()));
      }
    } else if (ADD_TO_EXISTING == action.getType()) {
      if (option == ADMINISTRATIVE_NOTE) {
        return holding -> holding.setAdministrativeNotes(holdingsNotesUpdater.addToAdministrativeNotes(action.getUpdated(), holding.getAdministrativeNotes()));
      } else if (option == HOLDINGS_NOTE) {
        return holding -> holding.setNotes(holdingsNotesUpdater.addToNotesByTypeId(holding.getNotes(), action.getParameters(), action.getUpdated()));
      }
    } else if (FIND_AND_REMOVE_THESE == action.getType()) {
      if (option == ADMINISTRATIVE_NOTE) {
        return holding -> holding.setAdministrativeNotes(holdingsNotesUpdater.findAndRemoveAdministrativeNote(action.getInitial(), holding.getAdministrativeNotes()));
      } else if (option == HOLDINGS_NOTE) {
        return holding -> holding.setNotes(holdingsNotesUpdater.findAndRemoveNoteByValueAndTypeId(action.getInitial(), holding.getNotes(), action.getParameters()));
      }
    } else if (FIND_AND_REPLACE == action.getType()) {
      if (option == ADMINISTRATIVE_NOTE) {
        return holding -> holding.setAdministrativeNotes(holdingsNotesUpdater.findAndReplaceAdministrativeNote(action, holding.getAdministrativeNotes()));
      } else if (option == HOLDINGS_NOTE) {
        return holding -> holdingsNotesUpdater.findAndReplaceNoteByValueAndTypeId(action, holding.getNotes());
      }
    } else if (CHANGE_TYPE == action.getType()) {
      if (option == ADMINISTRATIVE_NOTE) {
        return holding -> holdingsNotesUpdater.changeNoteTypeForAdministrativeNotes(holding, action);
      } else if (option == HOLDINGS_NOTE) {
        return holding -> holdingsNotesUpdater.changeNoteTypeForHoldingsNote(holding, action);
      }
    }
    return holding -> {
      throw new BulkOperationException(format("Combination %s and %s isn't supported yet", option, action.getType()));
    };
  }

  private boolean isSetDiscoverySuppressTrue(UpdateActionType actionType, UpdateOptionType optionType) {
    return (actionType == SET_TO_TRUE || actionType == SET_TO_TRUE_INCLUDING_ITEMS) && optionType == SUPPRESS_FROM_DISCOVERY;
  }

  private boolean isSetDiscoverySuppressFalse(UpdateActionType actionType, UpdateOptionType optionType) {
    return (actionType == SET_TO_FALSE || actionType == SET_TO_FALSE_INCLUDING_ITEMS) && optionType == SUPPRESS_FROM_DISCOVERY;
  }

  @Override
  public HoldingsRecord clone(HoldingsRecord entity) {
    var clone = entity.toBuilder().build();
    if (entity.getAdministrativeNotes() != null) {
      var administrativeNotes = new ArrayList<>(entity.getAdministrativeNotes());
      clone.setAdministrativeNotes(administrativeNotes);
    }
    if (entity.getNotes() != null) {
      var holdingsNotes = entity.getNotes().stream().map(note -> note.toBuilder().build()).toList();
      clone.setNotes(new ArrayList<>(holdingsNotes));
    }
    return clone;
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
