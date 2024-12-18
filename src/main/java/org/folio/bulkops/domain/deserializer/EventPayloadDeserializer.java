package org.folio.bulkops.domain.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.folio.bulkops.domain.bean.EventPayload;

import java.io.IOException;

public class EventPayloadDeserializer extends JsonDeserializer<EventPayload> {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Override
  public EventPayload deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
    var payload = jsonParser.readValueAs(String.class);
    return objectMapper.readValue(payload, EventPayload.class);
  }
}
