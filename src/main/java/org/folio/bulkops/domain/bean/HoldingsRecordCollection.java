package org.folio.bulkops.domain.bean;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.Valid;
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
public class HoldingsRecordCollection {
  @JsonProperty("holdingsRecords")
  @Valid
  private List<HoldingsRecord> holdingsRecords = null;

  @JsonProperty("totalRecords")
  private Integer totalRecords;

  @JsonProperty("resultInfo")
  private ResultInfo resultInfo;
}

