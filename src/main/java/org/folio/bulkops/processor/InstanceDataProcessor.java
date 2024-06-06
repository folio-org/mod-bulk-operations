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
import org.folio.bulkops.domain.bean.Instance;
import org.folio.bulkops.domain.dto.Action;
import org.folio.bulkops.domain.dto.UpdateOptionType;
import org.folio.bulkops.exception.BulkOperationException;
import org.folio.bulkops.exception.RuleValidationException;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Set;

@Log4j2
@Component
@RequiredArgsConstructor
public class InstanceDataProcessor extends AbstractDataProcessor<Instance> {
  private final InstanceNotesUpdaterFactory instanceNotesUpdaterFactory;

  @Override
  public Validator<UpdateOptionType, Action> validator(Instance instance) {
    return (option, action) -> {
      if (CLEAR_FIELD.equals(action.getType()) && Set.of(STAFF_SUPPRESS, SUPPRESS_FROM_DISCOVERY).contains(option)) {
        throw new RuleValidationException("Suppress flag cannot be cleared.");
      } else if (INSTANCE_NOTE.equals(option) && !"FOLIO".equals(instance.getSource())) {
        throw new RuleValidationException("Bulk edit of instance notes is not supported for MARC Instances.");
      } else if (ADMINISTRATIVE_NOTE.equals(option) && CHANGE_TYPE.equals(action.getType()) && !"FOLIO".equals(instance.getSource())) {
        throw new RuleValidationException("Change note type for administrative notes is not supported for MARC Instances.");
      }
    };
  }

  @Override
  public Updater<Instance> updater(UpdateOptionType option, Action action) {
    if (STAFF_SUPPRESS.equals(option)) {
      if (SET_TO_TRUE.equals(action.getType())) {
        return instance -> instance.setStaffSuppress(true);
      } else if (SET_TO_FALSE.equals(action.getType())) {
        return instance -> instance.setStaffSuppress(false);
      }
    } else if (SUPPRESS_FROM_DISCOVERY.equals(option)) {
      if (SET_TO_TRUE.equals(action.getType())) {
        return instance -> instance.setDiscoverySuppress(true);
      } else if (SET_TO_FALSE.equals(action.getType())) {
        return instance -> instance.setDiscoverySuppress(false);
      }
    }
    return instanceNotesUpdaterFactory.getUpdater(option, action).orElseGet(() -> instance -> {
      throw new BulkOperationException(format("Combination %s and %s isn't supported yet", option, action.getType()));
    });
  }

  @Override
  public Instance clone(Instance entity) {
    return entity.toBuilder().build();
  }

  @Override
  public boolean compare(Instance first, Instance second) {
    return Objects.equals(first, second);
  }

  @Override
  public Class<Instance> getProcessedType() {
    return Instance.class;
  }
}

