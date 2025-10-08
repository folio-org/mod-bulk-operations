package org.folio.bulkops.domain.bean;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.With;

@Data
@With
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class ReceivingHistoryEntries {
  @JsonProperty("displayType")
  private String displayType;

  @JsonProperty("entries")
  @Valid
  private List<ReceivingHistoryEntry> entries = null;
}

