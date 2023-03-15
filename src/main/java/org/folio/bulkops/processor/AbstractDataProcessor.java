package org.folio.bulkops.processor;

import java.util.function.Consumer;

import org.folio.bulkops.domain.bean.BulkOperationsEntity;
import org.folio.bulkops.domain.dto.Action;
import org.folio.bulkops.domain.dto.BulkOperationRule;
import org.folio.bulkops.domain.dto.BulkOperationRuleCollection;
import org.folio.bulkops.domain.dto.UpdateOptionType;
import org.folio.bulkops.service.ErrorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import lombok.extern.log4j.Log4j2;

@Log4j2
@Component
public abstract class AbstractDataProcessor<T extends BulkOperationsEntity> implements DataProcessor<T> {
  @Autowired
  private ErrorService errorService;

  @Override
  public UpdatedEntityHolder process(String identifier, T entity, BulkOperationRuleCollection rules) {
    var holder = UpdatedEntityHolder.builder().build();
    var updated = clone(entity);
    var preview = clone(entity);
    var countActions = 0;
    var countErrors = 0;
    for (BulkOperationRule rule : rules.getBulkOperationRules()) {
      var details = rule.getRuleDetails();
      var option = details.getOption();
      for (Action action : details.getActions()) {
        countActions++;
        try {
          try {
            validator(entity).validate(option, action);
          } catch (Exception e) {
            errorService.saveError(rule.getBulkOperationId(), identifier, e.getMessage());
            countErrors++;
            updater(option, action).apply(preview);
            continue;
          }
          updater(option, action).apply(updated);
          updater(option, action).apply(preview);
        } catch (Exception e) {
          countErrors++;
          log.error(String.format("%s id=%s, error: %s", updated.getClass().getSimpleName(), "id", e.getMessage()));
          errorService.saveError(rule.getBulkOperationId(), identifier, e.getMessage());
        }
      }
    }
    if (countErrors == 0 && compare(updated, entity)) {
      errorService.saveError(rules.getBulkOperationRules().get(0).getBulkOperationId(), identifier, "No change in value required");
    }
    if (countErrors < countActions) {
      holder.setShouldBeUpdated(true);
    }
    holder.setUpdated(updated);
    holder.setPreview(preview);
    return holder;
  }

  /**
   * Returns validator
   *
   * @param entity entity of type {@link T} to validate
   * @return true if {@link UpdateOptionType} and {@link Action} can be applied to entity
   */
  public abstract Validator<UpdateOptionType, Action> validator(T entity);

  /**
   * Returns {@link Consumer<T>} for applying changes for entity of type {@link T}
   *
   * @param option {@link UpdateOptionType} for update
   * @param action {@link Action} for update
   * @return updater
   */
  public abstract Updater<T> updater(UpdateOptionType option, Action action);

  /**
   * Clones object of type {@link T}
   *
   * @param entity object to clone
   * @return cloned object
   */
  public abstract T clone(T entity);

  /**
   * Compares objects of type {@link T}
   *
   * @param first  first object
   * @param second second object
   * @return true if objects are equal, otherwise - false
   */
  public abstract boolean compare(T first, T second);
}
