package org.folio.bulkops.mapper;

import java.util.List;

import org.folio.bulkops.domain.dto.BulkOperationDto;
import org.folio.bulkops.domain.entity.BulkOperation;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface BulkOperationMapper {
  BulkOperationDto mapToDto(BulkOperation bulkOperation);
  List<BulkOperationDto> mapToDtoList(List<BulkOperation> bulkOperations);
}
