package org.folio.bulkops.processor;

import java.util.function.Consumer;

import org.folio.bulkops.domain.bean.BulkOperationsEntity;
import org.folio.bulkops.domain.dto.Action;
import org.folio.bulkops.domain.dto.BulkOperationRule;
import org.folio.bulkops.domain.dto.BulkOperationRuleCollection;
import org.folio.bulkops.domain.dto.UpdateOptionType;
import org.folio.bulkops.exception.RuleValidationException;
import org.folio.bulkops.service.ConsortiaService;
import org.folio.bulkops.service.ErrorService;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.FolioModuleMetadata;
import org.folio.spring.scope.FolioExecutionContextSetter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import lombok.extern.log4j.Log4j2;

import static java.util.Objects.nonNull;
import static org.folio.bulkops.util.FolioExecutionContextUtil.prepareContextForTenant;

@Log4j2
@Component
public abstract class AbstractDataProcessor<T extends BulkOperationsEntity> implements DataProcessor<T> {
  @Autowired
  private ErrorService errorService;
  @Autowired
  private FolioExecutionContext folioExecutionContext;
  @Autowired
  private FolioModuleMetadata folioModuleMetadata;
  @Autowired
  private ConsortiaService consortiaService;

  @Override
  public UpdatedEntityHolder process(String identifier, T entity, BulkOperationRuleCollection rules) {
    var holder = UpdatedEntityHolder.builder().build();
    var updated = clone(entity);
    var preview = clone(entity);
    for (BulkOperationRule rule : rules.getBulkOperationRules()) {
      var details = rule.getRuleDetails();
      var option = details.getOption();
      var tenantsFromRule = rule.getRuleDetails().getTenants();
      log.info("tenantsFromRule: {}, entity.tenant: {}, entity.identifier: {}",
        tenantsFromRule, entity.getTenant(), entity.getIdentifier(org.folio.bulkops.domain.dto.IdentifierType.ID));
      if (nonNull(tenantsFromRule) && !tenantsFromRule.isEmpty() && !tenantsFromRule.contains(entity.getTenant())) {
        try (var ignored = new FolioExecutionContextSetter(prepareContextForTenant(consortiaService.getCentralTenantId(folioExecutionContext.getTenantId()), folioModuleMetadata, folioExecutionContext))) {
          errorService.saveError(rule.getBulkOperationId(), identifier,
            String.format("%s cannot be updated because the record is associated with %s and %s is not associated with this tenant.",
              entity.getIdentifier(org.folio.bulkops.domain.dto.IdentifierType.ID), entity.getTenant(), option.getValue()));
        }
        log.error(String.format("%s cannot be updated because the record is associated with %s and %s is not associated with this tenant.",
          entity.getIdentifier(org.folio.bulkops.domain.dto.IdentifierType.ID), entity.getTenant(), option.getValue()));
        continue;
      }
      for (Action action : details.getActions()) {
        var tenantsFromAction = action.getTenants();
        log.info("tenantsFromAction: {}, entity.tenant: {}, entity.identifier: {}",
          tenantsFromAction, entity.getTenant(), entity.getIdentifier(org.folio.bulkops.domain.dto.IdentifierType.ID));
        if (nonNull(tenantsFromAction) && !tenantsFromAction.isEmpty() && !tenantsFromAction.contains(entity.getTenant())) {
          log.info("current tenant: {}", consortiaService.getCentralTenantId(folioExecutionContext.getTenantId()));
          try (var ignored = new FolioExecutionContextSetter(prepareContextForTenant(consortiaService.getCentralTenantId(folioExecutionContext.getTenantId()), folioModuleMetadata, folioExecutionContext))) {
            errorService.saveError(rule.getBulkOperationId(), identifier,
              String.format("%s cannot be updated because the record is associated with %s and %s is not associated with this tenant.",
                entity.getIdentifier(org.folio.bulkops.domain.dto.IdentifierType.ID), entity.getTenant(), option.getValue()));
          }
          log.error(String.format("%s cannot be updated because the record is associated with %s and %s is not associated with this tenant.",
            entity.getIdentifier(org.folio.bulkops.domain.dto.IdentifierType.ID), entity.getTenant(), option.getValue()));
          continue;
        }
        try {
          updater(option, action).apply(preview);
          validator(entity).validate(option, action);
          updater(option, action).apply(updated);
        } catch (RuleValidationException e) {
          errorService.saveError(rule.getBulkOperationId(), identifier, e.getMessage());
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
