package org.folio.bulkops.domain.entity;

import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import org.folio.bulkops.domain.bean.Error;
import org.folio.bulkops.util.ErrorConverter;

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
@Table(name = "bulk_operation_errors")
public class BulkOperationError {
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private UUID id;

  private UUID bulkOperationId;

  @Column(name = "error_json")
  @Convert(converter = ErrorConverter.class)
  private Error error;
}
