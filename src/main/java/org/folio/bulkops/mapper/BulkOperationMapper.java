package org.folio.bulkops.mapper;

import org.folio.bulkops.domain.dto.BulkOperationDto;
import org.folio.bulkops.domain.entity.BulkOperation;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface BulkOperationMapper {
  BulkOperationDto mapToDto(BulkOperation bulkOperation);
  List<BulkOperationDto> mapToDtoList(List<BulkOperation> bulkOperations);
}
