package org.folio.bulkops.mapper;

import org.folio.bulkops.domain.dto.BulkOperationDto;
import org.folio.bulkops.domain.entity.BulkOperation;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface BulkOperationMapper {
//  @Mapping(target = "linkToOriginFile", ignore = true)
//  @Mapping(target = "linkToModifiedFile", ignore = true)
  BulkOperationDto mapToDto(BulkOperation bulkOperation);
  List<BulkOperationDto> mapToDtoList(List<BulkOperation> bulkOperations);
}
