package org.folio.bulkops.processor;

import org.folio.bulkops.exception.BulkOperationException;

@FunctionalInterface
public interface Updater<T> {
    void apply(T t) throws BulkOperationException;
}
