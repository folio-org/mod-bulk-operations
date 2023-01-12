package org.folio.bulkops.processor;

public interface UpdateProcessor<T> {
  void updateRecord(T t);
  Class<T> getUpdatedType();
}
