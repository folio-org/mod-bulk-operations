package org.folio.bulkops.domain.bean;

import com.fasterxml.jackson.annotation.JsonProperty;
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
public class UserPermissions {

  @JsonProperty("permissionNames")
  private List<String> permissionNames = new ArrayList<>();

  @JsonProperty("permissions")
  private List<String> permissions = new ArrayList<>();
}
