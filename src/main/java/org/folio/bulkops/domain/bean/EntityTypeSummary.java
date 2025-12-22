package org.folio.bulkops.domain.bean;

import java.util.Date;
import java.util.List;
import java.util.UUID;
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
public class EntityTypeSummary {
  private UUID id;
  private String label;
  private Boolean isCustom;
  private Boolean crossTenantQueriesEnabled = false;
  private List<String> missingPermissions;
  private Date createdAt;
  private Date updatedAt;
}
