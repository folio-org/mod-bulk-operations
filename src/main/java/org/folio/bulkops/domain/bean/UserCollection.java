package org.folio.bulkops.domain.bean;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import java.util.ArrayList;
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
public class UserCollection {
  @JsonProperty("users")
  @Valid
  private List<User> users = new ArrayList<>();

  @JsonProperty("totalRecords")
  private Integer totalRecords;

  @JsonProperty("resultInfo")
  private ResultInfo resultInfo;
}

