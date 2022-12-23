package org.folio.bulkops.processor;


import org.folio.bulkops.domain.dto.BulkOperationRuleCollection;
import org.folio.bulkops.exception.BulkOperationException;
import org.folio.bulkops.exception.RuleValidationException;

public interface DataProcessor<T> {
  /**
   * This method updates end returns entity of type {@link T} based on {@link BulkOperationRuleCollection}
   * @param entity original entity of type {@link T}
   * @param rule rule with updates
   * @return updated result
   */
  T process(T entity, BulkOperationRuleCollection rule) throws RuleValidationException, BulkOperationException;
}
