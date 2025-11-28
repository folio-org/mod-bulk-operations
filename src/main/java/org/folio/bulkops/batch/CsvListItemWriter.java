package org.folio.bulkops.batch;

import java.io.IOException;
import java.util.List;
import lombok.extern.log4j.Log4j2;
import org.folio.bulkops.domain.bean.BulkOperationsEntity;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemStream;
import org.springframework.batch.item.ItemWriter;

@Log4j2
public class CsvListItemWriter<T extends BulkOperationsEntity>
    implements ItemWriter<List<T>>, ItemStream {
  private final CsvItemWriter<T> delegate;

  public CsvListItemWriter(
      String path, Class<T> clazz, String bulkOperationId, String identifierType)
      throws IOException {
    delegate = new CsvItemWriter<>(path, clazz, bulkOperationId, identifierType);
  }

  @Override
  public void write(Chunk<? extends List<T>> chunk) throws Exception {
    delegate.write(new Chunk<>(chunk.getItems().stream().flatMap(List::stream).toList()));
  }

  @Override
  public void close() {
    delegate.close();
  }
}
