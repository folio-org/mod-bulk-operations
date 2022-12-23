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
public class ElectronicAccess {

  @JsonProperty("uri")
  private String uri;

  @JsonProperty("linkText")
  private String linkText;

  @JsonProperty("materialsSpecification")
  private String materialsSpecification;

  @JsonProperty("publicNote")
  private String publicNote;

  @JsonProperty("relationshipId")
  private String relationshipId;
}

