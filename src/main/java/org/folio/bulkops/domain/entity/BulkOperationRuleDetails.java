package org.folio.bulkops.domain.entity;

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.With;
import org.folio.bulkops.domain.dto.Parameter;
import org.folio.bulkops.domain.dto.UpdateActionType;
import org.hibernate.annotations.Type;

@Data
@Builder
@With
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "bulk_operation_rule_details")
public class BulkOperationRuleDetails {
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private UUID id;

  private UUID ruleId;

  @Enumerated(EnumType.STRING)
  private UpdateActionType updateAction;

  private String initialValue;
  private String updatedValue;

  @Type(JsonBinaryType.class)
  @Column(columnDefinition = "jsonb")
  private List<Parameter> parameters;

  private List<String> actionTenants;
  private List<String> ruleTenants;
  private List<String> updatedTenants;
}
