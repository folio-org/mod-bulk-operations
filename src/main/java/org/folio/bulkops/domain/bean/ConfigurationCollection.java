package org.folio.bulkops.domain.bean;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.With;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Data
@With
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class ConfigurationCollection {
  @JsonProperty("configs")
  @Valid
  private List<ModelConfiguration> configs = new ArrayList<>();

  @JsonProperty("totalRecords")
  private Integer totalRecords;

  @JsonProperty("resultInfo")
  private ResultInfo resultInfo;
}

