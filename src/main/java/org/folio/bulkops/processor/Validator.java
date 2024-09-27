package org.folio.bulkops.processor;

import org.folio.bulkops.exception.RuleValidationException;
import org.folio.bulkops.exception.RuleValidationTenantsException;

@FunctionalInterface
public interface Validator<T, U, V> {
    void validate(T t, U u, V v) throws RuleValidationException, RuleValidationTenantsException;
}
