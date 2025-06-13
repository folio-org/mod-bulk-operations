package org.folio.bulkops.batch;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.folio.bulkops.util.Constants.LINE_BREAK;

import lombok.SneakyThrows;
import org.folio.bulkops.domain.bean.BulkOperationsEntity;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.json.JacksonJsonObjectMarshaller;
import org.springframework.batch.item.json.JsonFileItemWriter;
import org.springframework.core.io.WritableResource;

public class JsonFileWriter<T extends BulkOperationsEntity> extends JsonFileItemWriter<T> {
  protected final JacksonJsonObjectMarshaller<T> marshaller;
  public JsonFileWriter(WritableResource resource) {
    super(resource, new JacksonJsonObjectMarshaller<>());
    lineSeparator = EMPTY;
    setHeaderCallback(writer -> writer.write(EMPTY));
    setFooterCallback(writer -> writer.write(EMPTY));
    marshaller = new JacksonJsonObjectMarshaller<>();
  }

  @SneakyThrows
  @Override
  public String doWrite(Chunk<? extends T> entities) {
    var lines = new StringBuilder();
    for (T entity : entities) {
      lines.append(marshaller.marshal(entity)).append(LINE_BREAK);
    }
    return lines.toString();
  }
}
