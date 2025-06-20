package org.folio.bulkops.mapper;

import org.folio.bulkops.domain.entity.Profile;
import org.folio.bulkops.domain.dto.ProfileRequest;
import org.folio.bulkops.domain.dto.ProfileUpdateRequest;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Builder;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.UUID;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, injectionStrategy = InjectionStrategy.CONSTRUCTOR,
  uses = {MappingMethods.class}, builder = @Builder(disableBuilder = true))
public interface ProfileRequestMapper {

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

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "createdDate", ignore = true)
  @Mapping(target = "createdBy", ignore = true)
  @Mapping(target = "createdByUser", ignore = true)
  @Mapping(target = "updatedDate", ignore = true)
  @Mapping(target = "updatedBy", ignore = true)
  @Mapping(target = "updatedByUser", ignore = true)
  void updateEntity(@MappingTarget Profile entity, ProfileUpdateRequest dto);
}
