package org.folio.bulkops.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.With;
import org.folio.bulkops.domain.dto.EntityType;
import org.folio.bulkops.domain.dto.MarcRuleDetails;
import org.folio.bulkops.domain.dto.RuleDetails;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Data
@Builder
@With
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "profile")
public class Profile implements Serializable {
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private UUID id;

  @Column(name = "name")
  private String name;

  @Column(name = "description")
  private String description;

  @Column(name = "locked")
  private boolean locked;

  @Enumerated(EnumType.STRING)
  @Column(name = "entity_type")
  private EntityType entityType;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "bulk_operation_rule_collection", columnDefinition = "jsonb")
  private List<RuleDetails> ruleDetails;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "bulk_operation_marc_rule_collection", columnDefinition = "jsonb")
  private List<MarcRuleDetails> marcRuleDetails;

  @Column(name = "created_date")
  private OffsetDateTime createdDate;

  @Column(name = "created_by")
  private UUID createdBy;

  @Column(name = "updated_date")
  private OffsetDateTime updatedDate;

  @Column(name = "updated_by")
  private UUID updatedBy;
}
