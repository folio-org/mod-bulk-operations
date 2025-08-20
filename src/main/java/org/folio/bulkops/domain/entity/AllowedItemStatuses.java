package org.folio.bulkops.domain.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Singular;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "allowed_item_statuses")
public class AllowedItemStatuses {
  @Id
  private String status;

  @Singular
  private List<String> allowedStatuses;
}
