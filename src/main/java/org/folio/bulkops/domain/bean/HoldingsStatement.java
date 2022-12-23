package org.folio.bulkops.domain.bean;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.With;

import javax.annotation.Generated;
import java.util.Objects;

@Data
@With
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class HoldingsStatement {
  @JsonProperty("statement")
  private String statement;

  @JsonProperty("note")
  private String note;

  @JsonProperty("staffNote")
  private String staffNote;
}

