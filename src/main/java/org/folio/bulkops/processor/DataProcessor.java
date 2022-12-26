package org.folio.bulkops.processor;


import org.folio.bulkops.domain.dto.BulkOperationRuleCollection;

public interface DataProcessor<T> {
  /**
   * This method updates end returns entity of type {@link T} based on {@link BulkOperationRuleCollection}
   *
   * @param entity original entity of type {@link T}
   * @param rule   rule with updates
   * @return updated result
   */
  T process(String identifier, T entity, BulkOperationRuleCollection rule);

  Class<T> getProcessedType();
}
