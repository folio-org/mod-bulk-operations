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
import org.folio.bulkops.domain.dto.MarcAction;
import org.folio.bulkops.domain.dto.MarcParameter;
import org.folio.bulkops.domain.dto.MarcSubfieldAction;
import org.folio.bulkops.domain.dto.UpdateOptionType;
import org.hibernate.annotations.Type;

@Data
@Builder
@With
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "bulk_operation_marc_rule")
public class BulkOperationMarcRule {
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private UUID id;

  private UUID bulkOperationId;
  private UUID userId;
  private String tag;
  private String ind1;
  private String ind2;
  private String subfield;

  @Enumerated(EnumType.STRING)
  private UpdateOptionType updateOption;

  @Type(JsonBinaryType.class)
  @Column(columnDefinition = "jsonb")
  private List<MarcAction> actions;

  @Type(JsonBinaryType.class)
  @Column(columnDefinition = "jsonb")
  private List<MarcParameter> parameters;

  @Type(JsonBinaryType.class)
  @Column(columnDefinition = "jsonb")
  private List<MarcSubfieldAction> subfields;
}
