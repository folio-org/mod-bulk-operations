package org.folio.bulkops.processor;

import static java.lang.String.format;
import static org.folio.bulkops.domain.dto.UpdateActionType.CHANGE_TYPE;
import static org.folio.bulkops.domain.dto.UpdateActionType.CLEAR_FIELD;
import static org.folio.bulkops.domain.dto.UpdateActionType.SET_TO_FALSE;
import static org.folio.bulkops.domain.dto.UpdateActionType.SET_TO_TRUE;
import static org.folio.bulkops.domain.dto.UpdateOptionType.ADMINISTRATIVE_NOTE;
import static org.folio.bulkops.domain.dto.UpdateOptionType.INSTANCE_NOTE;
import static org.folio.bulkops.domain.dto.UpdateOptionType.STAFF_SUPPRESS;
import static org.folio.bulkops.domain.dto.UpdateOptionType.SUPPRESS_FROM_DISCOVERY;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.folio.bulkops.domain.bean.ExtendedInstance;
import org.folio.bulkops.domain.dto.Action;
import org.folio.bulkops.domain.dto.UpdateOptionType;
import org.folio.bulkops.domain.dto.BulkOperationRule;
import org.folio.bulkops.exception.BulkOperationException;
import org.folio.bulkops.exception.RuleValidationException;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Objects;
import java.util.Set;

@Log4j2
@Component
@RequiredArgsConstructor
public class FolioInstanceDataProcessor extends AbstractDataProcessor<ExtendedInstance> {

  private final InstanceNotesUpdaterFactory instanceNotesUpdaterFactory;

  @Override
  public Validator<UpdateOptionType, Action, BulkOperationRule> validator(ExtendedInstance extendedInstance) {
    return (option, action, rule) -> {
      if (CLEAR_FIELD.equals(action.getType()) && Set.of(STAFF_SUPPRESS, SUPPRESS_FROM_DISCOVERY).contains(option)) {
        throw new RuleValidationException("Suppress flag cannot be cleared.");
      } else if (INSTANCE_NOTE.equals(option) && !"FOLIO".equals(extendedInstance.getEntity().getSource())) {
        throw new RuleValidationException("Bulk edit of instance notes is not supported for MARC Instances.");
      } else if (ADMINISTRATIVE_NOTE.equals(option) && CHANGE_TYPE.equals(action.getType()) && !"FOLIO".equals(extendedInstance.getEntity().getSource())) {
        throw new RuleValidationException("Change note type for administrative notes is not supported for MARC Instances.");
      }
    };
  }

  @Override
  public Updater<ExtendedInstance> updater(UpdateOptionType option, Action action, ExtendedInstance entity, boolean forPreview) {
    if (STAFF_SUPPRESS.equals(option)) {
      if (SET_TO_TRUE.equals(action.getType())) {
        return extendedInstance -> extendedInstance.getEntity().setStaffSuppress(true);
      } else if (SET_TO_FALSE.equals(action.getType())) {
        return extendedInstance -> extendedInstance.getEntity().setStaffSuppress(false);
      }
    } else if (SUPPRESS_FROM_DISCOVERY.equals(option)) {
      if (SET_TO_TRUE.equals(action.getType())) {
        return extendedInstance -> extendedInstance.getEntity().setDiscoverySuppress(true);
      } else if (SET_TO_FALSE.equals(action.getType())) {
        return extendedInstance -> extendedInstance.getEntity().setDiscoverySuppress(false);
      }
    }
    return instanceNotesUpdaterFactory.getUpdater(option, action).orElseGet(() -> instance -> {
      throw new BulkOperationException(format("Combination %s and %s isn't supported yet", option, action.getType()));
    });
  }

  @Override
  public ExtendedInstance clone(ExtendedInstance extendedInstance) {
    var instance = extendedInstance.getEntity();
    var clone = extendedInstance.getEntity().toBuilder().build();
    if (instance.getAdministrativeNotes() != null) {
      clone.setAdministrativeNotes(new ArrayList<>(instance.getAdministrativeNotes()));
    }
    if (instance.getInstanceNotes() != null) {
      clone.setInstanceNotes(new ArrayList<>(
        instance.getInstanceNotes().stream()
        .map(instanceNote -> instanceNote.toBuilder().build())
        .toList()));
    }
    return ExtendedInstance.builder().tenantId(extendedInstance.getTenantId()).entity(clone).build();
  }

  @Override
  public boolean compare(ExtendedInstance first, ExtendedInstance second) {
    if (StringUtils.equals(first.getTenantId(), second.getTenantId())) {
      return Objects.equals(first.getEntity(), second.getEntity());
    }
    return false;
  }

  @Override
  public Class<ExtendedInstance> getProcessedType() {
    return ExtendedInstance.class;
  }
}

