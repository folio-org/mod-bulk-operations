package org.folio.bulkops.batch.jobs.processidentifiers;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.listener.ItemWriteListener;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ListIdentifiersWriteListener<T> implements ItemWriteListener<List<T>> {
  private final IdentifiersWriteListener<T> delegate;

  @Override
  public void afterWrite(Chunk<? extends List<T>> list) {
    var chunk = new Chunk<>(list.getItems().stream().flatMap(List::stream).toList());
    delegate.afterWrite(chunk);
  }
}
