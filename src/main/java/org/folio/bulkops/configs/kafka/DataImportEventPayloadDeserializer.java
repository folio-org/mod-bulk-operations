package org.folio.bulkops.configs.kafka;

import java.nio.charset.StandardCharsets;
import org.apache.kafka.common.header.Headers;
import org.folio.bulkops.configs.kafka.dto.Event;
import org.jspecify.annotations.NonNull;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

public class DataImportEventPayloadDeserializer<T> extends JacksonJsonDeserializer<T> {

  private final ObjectMapper objectMapper;

  public DataImportEventPayloadDeserializer(JavaType javaType, JsonMapper objectMapper, boolean b) {
    super(javaType, objectMapper, b);
    this.objectMapper = objectMapper;
  }

  @Override
  public T deserialize(@NonNull String topic, @NonNull Headers headers, byte[] data) {
    var event = objectMapper.readValue(data, Event.class);
    return super.deserialize(
        topic, headers, event.getEventPayload().getBytes(StandardCharsets.UTF_8));
  }
}
