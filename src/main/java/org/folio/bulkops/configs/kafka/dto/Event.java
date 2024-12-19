package org.folio.bulkops.configs.kafka.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.With;

/**
 * Kafka event specific format used for Data Import module.
 */
@Data
@With
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class Event {
  @JsonProperty("id")
  private String id;
  @JsonProperty("eventType")
  private String eventType;
  @JsonProperty("eventPayload")
  private String eventPayload;
}
