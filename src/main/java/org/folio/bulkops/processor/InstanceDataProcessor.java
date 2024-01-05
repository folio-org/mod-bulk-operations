package org.folio.bulkops.processor;

import static java.lang.String.format;
import static org.folio.bulkops.domain.dto.UpdateActionType.CLEAR_FIELD;
import static org.folio.bulkops.domain.dto.UpdateActionType.SET_TO_FALSE;
import static org.folio.bulkops.domain.dto.UpdateActionType.SET_TO_TRUE;
import static org.folio.bulkops.domain.dto.UpdateOptionType.STAFF_SUPPRESS;

import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.bulkops.domain.bean.Instance;
import org.folio.bulkops.domain.dto.Action;
import org.folio.bulkops.domain.dto.UpdateOptionType;
import org.folio.bulkops.exception.BulkOperationException;
import org.folio.bulkops.exception.RuleValidationException;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Log4j2
@Component
@AllArgsConstructor
public class InstanceDataProcessor extends AbstractDataProcessor<Instance> {
  @Override
  public Validator<UpdateOptionType, Action> validator(Instance instance) {
    return (option, action) -> {
      if (CLEAR_FIELD.equals(action.getType()) && STAFF_SUPPRESS.equals(option)) {
        throw new RuleValidationException("Staff suppress flag cannot be cleared");
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
    }
    return item -> {
      throw new BulkOperationException(format("Combination %s and %s isn't supported yet", option, action.getType()));
    };
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

