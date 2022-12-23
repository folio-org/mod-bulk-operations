package org.folio.bulkops.processor;

import org.folio.bulkops.exception.RuleValidationException;

import java.io.IOException;

@FunctionalInterface
public interface Validator<T, U> {
    void validate(T t, U u) throws RuleValidationException;
}
