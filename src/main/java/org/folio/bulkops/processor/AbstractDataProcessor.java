package org.folio.bulkops.processor;

import lombok.extern.log4j.Log4j2;
import org.folio.bulkops.domain.dto.Action;
import org.folio.bulkops.domain.dto.BulkOperationRule;
import org.folio.bulkops.domain.dto.BulkOperationRuleCollection;
import org.folio.bulkops.domain.dto.UpdateOptionType;
import org.folio.bulkops.service.ErrorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

@Log4j2
@Component
public abstract class AbstractDataProcessor<T> implements DataProcessor<T> {
  @Autowired
  private ErrorService errorService;

  @Override
  public T process(String identifier, T entity, BulkOperationRuleCollection rules) {
    var hasProcessingError = false;
    var updated = clone(entity);
    for (BulkOperationRule rule : rules.getBulkOperationRules()) {
      var details = rule.getRuleDetails();
      var option = details.getOption();
      for (Action action : details.getActions()) {
        try {
          validator(updated).validate(option, action);
          updater(option, action).apply(updated);
        } catch (Exception e) {
          hasProcessingError = true;
          log.error(String.format("%s id=%s, error: %s", updated.getClass().getSimpleName(), "id", e.getMessage()));
          errorService.saveError(rule.getBulkOperationId(), identifier, e.getMessage());
        }
      }
    }
    if (compare(updated, entity)) {
      if (!hasProcessingError) {
        errorService.saveError(rules.getBulkOperationRules().get(0).getBulkOperationId(), identifier, "No change in value required");
      }
      return null;
    }
    return updated;
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
