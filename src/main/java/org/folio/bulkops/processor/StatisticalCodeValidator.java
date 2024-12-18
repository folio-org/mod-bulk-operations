package org.folio.bulkops.processor;

import org.folio.bulkops.exception.RuleValidationException;

@FunctionalInterface
public interface StatisticalCodeValidator<T> {
  void validate(T t) throws RuleValidationException;
}
