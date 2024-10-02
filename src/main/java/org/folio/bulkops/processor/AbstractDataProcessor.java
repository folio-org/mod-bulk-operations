package org.folio.bulkops.processor;

import java.util.function.Consumer;

import org.folio.bulkops.domain.bean.BulkOperationsEntity;
import org.folio.bulkops.domain.dto.Action;
import org.folio.bulkops.domain.dto.BulkOperationRule;
import org.folio.bulkops.domain.dto.BulkOperationRuleCollection;
import org.folio.bulkops.domain.dto.UpdateOptionType;
import org.folio.bulkops.exception.RuleValidationException;
import org.folio.bulkops.exception.RuleValidationTenantsException;
import org.folio.bulkops.service.ConsortiaService;
import org.folio.bulkops.service.ErrorService;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.FolioModuleMetadata;
import org.folio.spring.scope.FolioExecutionContextSetter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import lombok.extern.log4j.Log4j2;

import static org.folio.bulkops.util.FolioExecutionContextUtil.prepareContextForTenant;

@Log4j2
@Component
public abstract class AbstractDataProcessor<T extends BulkOperationsEntity> implements DataProcessor<T> {
  @Autowired
  private ErrorService errorService;
  @Autowired
  private FolioModuleMetadata folioModuleMetadata;
  @Autowired
  private ConsortiaService consortiaService;
  @Autowired
  protected FolioExecutionContext folioExecutionContext;

  @Override
  public UpdatedEntityHolder process(String identifier, T entity, BulkOperationRuleCollection rules) {
    var holder = UpdatedEntityHolder.builder().build();
    var updated = clone(entity);
    var preview = clone(entity);
    for (BulkOperationRule rule : rules.getBulkOperationRules()) {
      var details = rule.getRuleDetails();
      var option = details.getOption();
      for (Action action : details.getActions()) {
        try {
          updater(option, action, entity, rule, true).apply(preview);
          validator(entity).validate(option, action, rule);
          updater(option, action, entity, rule, false).apply(updated);
        } catch (RuleValidationException e) {
          errorService.saveError(rule.getBulkOperationId(), identifier, e.getMessage());
        } catch (RuleValidationTenantsException e) {
          try (var ignored = new FolioExecutionContextSetter(prepareContextForTenant(consortiaService.getCentralTenantId(folioExecutionContext.getTenantId()), folioModuleMetadata, folioExecutionContext))) {
            log.info("current tenant: {}", folioExecutionContext.getTenantId());
            errorService.saveError(rule.getBulkOperationId(), identifier, e.getMessage());
          }
          log.error(e.getMessage());
        } catch (Exception e) {
          log.error(String.format("%s id=%s, error: %s", updated.getRecordBulkOperationEntity().getClass().getSimpleName(), "id", e.getMessage()));
          errorService.saveError(rule.getBulkOperationId(), identifier, e.getMessage());
        }
      }
    }
    holder.setUpdated(updated);
    holder.setPreview(preview);
    return holder;
  }

  /**
   * Returns validator
   *
   * @param entity entity of type {@link T} to validate
   * @return true if {@link UpdateOptionType} and {@link Action}, and {@link BulkOperationRule} can be applied to entity
   */
  public abstract Validator<UpdateOptionType, Action, BulkOperationRule> validator(T entity);

  /**
   * Returns {@link Consumer<T>} for applying changes for entity of type {@link T}
   *
   * @param option {@link UpdateOptionType} for update
   * @param action {@link Action} for update
   * @param action {@link T} for update
   * @param action {@link BulkOperationRule} for update
   * @return updater
   */
  public abstract Updater<T> updater(UpdateOptionType option, Action action, T entity, BulkOperationRule rule, boolean forPreview) throws RuleValidationTenantsException;

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
