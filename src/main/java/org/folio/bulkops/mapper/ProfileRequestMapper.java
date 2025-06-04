package org.folio.bulkops.mapper;

import org.folio.bulkops.domain.entity.Profile;
import org.folio.bulkops.domain.dto.ProfileRequest;
import org.mapstruct.*;

import java.util.UUID;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, injectionStrategy = InjectionStrategy.CONSTRUCTOR,
  uses = {MappingMethods.class}, builder = @Builder(disableBuilder = true))
public interface ProfileRequestMapper {
//  @Mapping(target = "id", expression = "java(java.util.UUID.randomUUID())")
  @Mapping(target = "name", source = "dto.name")
  @Mapping(target = "description", source = "dto.description")
  @Mapping(target = "locked", source = "dto.locked")
  @Mapping(target = "entityType", source = "dto.entityType")
  @Mapping(target = "bulkOperationRuleCollection", source = "dto.bulkOperationRuleCollection")
  @Mapping(target = "bulkOperationMarcRuleCollection", source = "dto.bulkOperationMarcRuleCollection")
  @Mapping(target = "createdDate", expression = "java(java.time.OffsetDateTime.now())")
  @Mapping(target = "createdBy", source = "createdById")
  @Mapping(target = "createdByUser", source = "createdByUsername")
  @Mapping(target = "updatedDate", expression = "java(java.time.OffsetDateTime.now())")
  @Mapping(target = "updatedBy", source = "createdById")
  @Mapping(target = "updatedByUser", source = "createdByUsername")
  Profile toEntity(ProfileRequest dto, UUID createdById, String createdByUsername);

}
