package org.folio.bulkops.batch;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.folio.bulkops.util.Constants.LINE_BREAK;

import java.util.List;
import org.folio.bulkops.domain.bean.BulkOperationsEntity;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.json.JacksonJsonObjectMarshaller;
import org.springframework.batch.infrastructure.item.json.JsonFileItemWriter;
import org.springframework.core.io.WritableResource;
import org.springframework.lang.NonNull;

public class JsonListFileWriter<T extends BulkOperationsEntity>
    extends JsonFileItemWriter<List<T>> {
  private final JacksonJsonObjectMarshaller<T> marshaller;

  public JsonListFileWriter(WritableResource resource) {
    super(resource, new JacksonJsonObjectMarshaller<>());
    lineSeparator = EMPTY;
    setHeaderCallback(writer -> writer.write(EMPTY));
    setFooterCallback(writer -> writer.write(EMPTY));
    marshaller = new JacksonJsonObjectMarshaller<>();
  }

  @Override
  public @NonNull String doWrite(Chunk<? extends List<T>> lists) {
    var lines = new StringBuilder();
    var chunk = new Chunk<>(lists.getItems().stream().flatMap(List::stream).toList());
    for (T entity : chunk) {
      lines.append(marshaller.marshal(entity)).append(LINE_BREAK);
    }
    return lines.toString();
  }
}
