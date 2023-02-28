package org.folio.bulkops.domain.entity;

import java.util.UUID;

import org.folio.bulkops.domain.dto.UpdateActionType;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.With;

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
}
