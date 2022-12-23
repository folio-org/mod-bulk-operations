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
public class EffectiveCallNumberComponents {
  @JsonProperty("callNumber")
  private String callNumber;

  @JsonProperty("prefix")
  private String prefix;

  @JsonProperty("suffix")
  private String suffix;

  @JsonProperty("typeId")
  private String typeId;
}

