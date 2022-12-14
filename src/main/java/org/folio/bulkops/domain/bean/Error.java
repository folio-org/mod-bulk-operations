package org.folio.bulkops.domain.bean;

import java.util.List;

import javax.validation.Valid;

import com.fasterxml.jackson.annotation.JsonProperty;

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
public class Error   {
  @JsonProperty("message")
  private String message;

  @JsonProperty("type")
  private String type;

  @JsonProperty("code")
  private String code;

  @JsonProperty("parameters")
  @Valid
  private List<Parameter> parameters = null;
}

