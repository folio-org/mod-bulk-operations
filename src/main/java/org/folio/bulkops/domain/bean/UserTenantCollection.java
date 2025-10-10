package org.folio.bulkops.domain.bean;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserTenantCollection {

  @JsonProperty("userTenants")
  private List<UserTenant> userTenants = new ArrayList<>();
}
