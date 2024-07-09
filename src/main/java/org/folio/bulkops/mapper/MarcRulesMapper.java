package org.folio.bulkops.mapper;

import org.folio.bulkops.domain.entity.BulkOperationMarcRule;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface MarcRulesMapper {
  org.folio.bulkops.domain.dto.BulkOperationMarcRule mapToDto(BulkOperationMarcRule rule);
  BulkOperationMarcRule mapToEntity(org.folio.bulkops.domain.dto.BulkOperationMarcRule rule);
}
