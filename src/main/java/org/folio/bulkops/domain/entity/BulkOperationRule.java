package org.folio.bulkops.domain.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.Table;

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
