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

import org.apache.commons.lang3.StringUtils;
import org.folio.bulkops.domain.bean.ExtendedHoldingsRecord;
import org.folio.bulkops.domain.dto.Action;
import org.folio.bulkops.domain.dto.UpdateActionType;
import org.folio.bulkops.domain.dto.UpdateOptionType;
import org.folio.bulkops.exception.BulkOperationException;
import org.folio.bulkops.exception.NotFoundException;
import org.folio.bulkops.exception.RuleValidationException;
import org.folio.bulkops.service.HoldingsReferenceService;
import org.folio.bulkops.service.ItemReferenceService;
import org.folio.bulkops.service.ElectronicAccessReferenceService;
import org.springframework.stereotype.Component;

import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Log4j2
@Component
@AllArgsConstructor
public class HoldingsDataProcessor extends AbstractDataProcessor<ExtendedHoldingsRecord> {


  private final ItemReferenceService itemReferenceService;
  private final HoldingsReferenceService holdingsReferenceService;
  private final HoldingsNotesUpdater holdingsNotesUpdater;
  private final ElectronicAccessUpdaterFactory electronicAccessUpdaterFactory;
  private final ElectronicAccessReferenceService electronicAccessReferenceService;

  private static final Pattern UUID_REGEX =
    Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");

  @Override
  public Validator<UpdateOptionType, Action> validator(ExtendedHoldingsRecord extendedHoldingsRecord) {
    return (option, action) -> {
      try {
        if ("MARC".equals(holdingsReferenceService.getSourceById(extendedHoldingsRecord.getEntity().getSourceId()).getName())) {
          throw new RuleValidationException("Holdings records that have source \"MARC\" cannot be changed");
        }
      } catch (NotFoundException e) {
        log.error("Holdings source was not found by id={}", extendedHoldingsRecord.getEntity().getSourceId());
      }
      if (REPLACE_WITH == action.getType()) {
        validateReplacement(option, action);
      }
      if (PERMANENT_LOCATION == option && CLEAR_FIELD == action.getType()) {
        throw new RuleValidationException("Permanent location cannot be cleared");
      }
    };
  }

  public Updater<ExtendedHoldingsRecord> updater(UpdateOptionType option, Action action) {
    if (isElectronicAccessUpdate(option)) {
      return (Updater<ExtendedHoldingsRecord>) electronicAccessUpdaterFactory.updater(option, action);
    } else if (REPLACE_WITH == action.getType()) {
      return extendedHoldingsRecord -> {
        var locationId = action.getUpdated();
        if (PERMANENT_LOCATION == option) {
          extendedHoldingsRecord.getEntity().setPermanentLocationId(locationId);
          extendedHoldingsRecord.getEntity().setEffectiveLocationId(isEmpty(extendedHoldingsRecord.getEntity().getTemporaryLocationId()) ? locationId : extendedHoldingsRecord.getEntity().getTemporaryLocationId());
        } else {
          extendedHoldingsRecord.getEntity().setTemporaryLocationId(locationId);
          extendedHoldingsRecord.getEntity().setEffectiveLocationId(locationId);
        }
      };
    } else if (CLEAR_FIELD == action.getType()) {
      return extendedHoldingsRecord -> {
        extendedHoldingsRecord.getEntity().setTemporaryLocationId(null);
        extendedHoldingsRecord.getEntity().setEffectiveLocationId(extendedHoldingsRecord.getEntity().getPermanentLocationId());
      };
    } else if (isSetDiscoverySuppressTrue(action.getType(), option)) {
      return extendedHoldingsRecord -> extendedHoldingsRecord.getEntity().setDiscoverySuppress(true);
    } else if (isSetDiscoverySuppressFalse(action.getType(), option)) {
      return extendedHoldingsRecord -> extendedHoldingsRecord.getEntity().setDiscoverySuppress(false);
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
          electronicAccessReferenceService.getRelationshipNameById(newId);
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
  public ExtendedHoldingsRecord clone(ExtendedHoldingsRecord extendedEntity) {
    var entity = extendedEntity.getEntity();
    var clone = entity.toBuilder().build();
    if (entity.getAdministrativeNotes() != null) {
      var administrativeNotes = new ArrayList<>(entity.getAdministrativeNotes());
      clone.setAdministrativeNotes(administrativeNotes);
    }
    if (entity.getNotes() != null) {
      var holdingsNotes = entity.getNotes().stream().map(note -> note.toBuilder().build()).toList();
      clone.setNotes(new ArrayList<>(holdingsNotes));
    }

    return ExtendedHoldingsRecord.builder().tenantId(extendedEntity.getTenantId()).entity(clone).build();
  }

  @Override
  public boolean compare(ExtendedHoldingsRecord first, ExtendedHoldingsRecord second) {
    if (StringUtils.equals(first.getTenantId(), second.getTenantId())) {
      return Objects.equals(first.getEntity(), second.getEntity());
    }
    return false;
  }

  @Override
  public Class<ExtendedHoldingsRecord> getProcessedType() {
    return ExtendedHoldingsRecord.class;
  }
}
