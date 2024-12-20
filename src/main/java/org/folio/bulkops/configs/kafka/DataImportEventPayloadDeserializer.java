package org.folio.bulkops.configs.kafka;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.header.Headers;
import org.folio.bulkops.configs.kafka.dto.Event;
import org.springframework.kafka.support.serializer.DeserializationException;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class DataImportEventPayloadDeserializer<T> extends JsonDeserializer<T> {

  private final ObjectMapper objectMapper;

  public DataImportEventPayloadDeserializer(JavaType javaType, ObjectMapper objectMapper, boolean b) {
    super(javaType, objectMapper, b);
    this.objectMapper = objectMapper;
  }

  @Override
  public T deserialize(String topic, Headers headers, byte[] data) {
    try {
      var event = objectMapper.readValue(data, Event.class);
      return super.deserialize(topic, headers, event.getEventPayload().getBytes(StandardCharsets.UTF_8));
    } catch (IOException e) {
      throw new DeserializationException("Event deserialization error", data, false, e);
    }
  }
}
