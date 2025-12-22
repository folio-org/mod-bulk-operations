package org.folio.bulkops.domain.bean;

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
public class EntityTypeSummaries {
  private List<@Valid EntityTypeSummary> entityTypes = new ArrayList<>();
  private String version;
}
