package org.folio.bulkops.processor;

import static java.lang.String.format;
import static org.apache.commons.lang3.ObjectUtils.isEmpty;
import static org.folio.bulkops.domain.dto.UpdateActionType.CLEAR_FIELD;
import static org.folio.bulkops.domain.dto.UpdateActionType.REPLACE_WITH;
import static org.folio.bulkops.domain.dto.UpdateActionType.SET_TO_FALSE;
import static org.folio.bulkops.domain.dto.UpdateActionType.SET_TO_FALSE_INCLUDING_ITEMS;
import static org.folio.bulkops.domain.dto.UpdateActionType.SET_TO_TRUE;
import static org.folio.bulkops.domain.dto.UpdateActionType.SET_TO_TRUE_INCLUDING_ITEMS;
import static org.folio.bulkops.domain.dto.UpdateOptionType.ELECTRONIC_ACCESS_LINK_TEXT;
import static org.folio.bulkops.domain.dto.UpdateOptionType.ELECTRONIC_ACCESS_MATERIALS_SPECIFIED;
import static org.folio.bulkops.domain.dto.UpdateOptionType.ELECTRONIC_ACCESS_URI;
import static org.folio.bulkops.domain.dto.UpdateOptionType.ELECTRONIC_ACCESS_URL_PUBLIC_NOTE;
import static org.folio.bulkops.domain.dto.UpdateOptionType.ELECTRONIC_ACCESS_URL_RELATIONSHIP;
import static org.folio.bulkops.domain.dto.UpdateOptionType.PERMANENT_LOCATION;
import static org.folio.bulkops.domain.dto.UpdateOptionType.SUPPRESS_FROM_DISCOVERY;
import static org.folio.bulkops.domain.dto.UpdateOptionType.TEMPORARY_LOCATION;

import java.util.ArrayList;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

import org.folio.bulkops.domain.bean.HoldingsRecord;
import org.folio.bulkops.domain.dto.Action;
import org.folio.bulkops.domain.dto.UpdateActionType;
import org.folio.bulkops.domain.dto.UpdateOptionType;
import org.folio.bulkops.exception.BulkOperationException;
import org.folio.bulkops.exception.NotFoundException;
import org.folio.bulkops.exception.RuleValidationException;
import org.folio.bulkops.service.ElectronicAccessService;
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
  private final ElectronicAccessUpdaterFactory electronicAccessUpdaterFactory;
  private final ElectronicAccessService electronicAccessService;

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
        validateReplacement(option, action);
      }
      if (PERMANENT_LOCATION == option && CLEAR_FIELD == action.getType()) {
        throw new RuleValidationException("Permanent location cannot be cleared");
      }
    };
  }

  public Updater<HoldingsRecord> updater(UpdateOptionType option, Action action) {
    if (isElectronicAccessUpdate(option)) {
      return (Updater<HoldingsRecord>) electronicAccessUpdaterFactory.updater(option, action);
    } else if (REPLACE_WITH == action.getType()) {
      return holding -> {
        var locationId = action.getUpdated();
        if (PERMANENT_LOCATION == option) {
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
    }
    var notesUpdaterOptional = holdingsNotesUpdater.updateNotes(action, option);
    if (notesUpdaterOptional.isPresent()) return notesUpdaterOptional.get();
    return holding -> {
      throw new BulkOperationException(format("Combination %s and %s isn't supported yet", option, action.getType()));
    };
  }

  private void validateReplacement(UpdateOptionType option, Action action) throws RuleValidationException {
    if (isIdValue(option)) {
      var newId = action.getUpdated();
      if (isEmpty(newId)) {
        throw new RuleValidationException("Id value cannot be empty");
      }
      if (!UUID_REGEX.matcher(action.getUpdated()).matches()) {
        throw new RuleValidationException("UUID has invalid format: %s" + newId);
      }

      if (Set.of(PERMANENT_LOCATION, TEMPORARY_LOCATION).contains(option)) {
        try {
          itemReferenceService.getLocationById(newId);
        } catch (Exception e) {
          throw new RuleValidationException(format("Location %s doesn't exist", newId));
        }
      } else if (ELECTRONIC_ACCESS_URL_RELATIONSHIP.equals(option)) {
        try {
          electronicAccessService.getRelationshipNameAndIdById(newId);
        } catch (Exception e) {
          throw new RuleValidationException(format("URL relationship %s doesn't exist", newId));
        }
      }
    }
  }

  private boolean isIdValue(UpdateOptionType option) {
    return Set.of(PERMANENT_LOCATION,
      TEMPORARY_LOCATION,
      ELECTRONIC_ACCESS_URL_RELATIONSHIP).contains(option);
  }

  private boolean isElectronicAccessUpdate(UpdateOptionType option) {
    return Set.of(ELECTRONIC_ACCESS_URL_RELATIONSHIP,
      ELECTRONIC_ACCESS_URI,
      ELECTRONIC_ACCESS_LINK_TEXT,
      ELECTRONIC_ACCESS_MATERIALS_SPECIFIED,
      ELECTRONIC_ACCESS_URL_PUBLIC_NOTE).contains(option);
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
