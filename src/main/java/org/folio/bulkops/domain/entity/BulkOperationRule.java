package org.folio.bulkops.domain.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.OneToMany;

import org.folio.bulkops.domain.dto.UpdateOptionType;

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
@Table(name = "bulk_operation_rule")
public class BulkOperationRule {
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private UUID id;

  private UUID bulkOperationId;
  private UUID userId;

  @Enumerated(EnumType.STRING)
  private UpdateOptionType updateOption;

  @OneToMany(fetch = FetchType.EAGER)
  @JoinColumn(name = "ruleId")
  private List<BulkOperationRuleDetails> ruleDetails = new ArrayList<>();
}
