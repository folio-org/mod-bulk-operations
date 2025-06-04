package org.folio.bulkops.mapper;

import org.folio.bulkops.domain.entity.Profile;
import org.folio.bulkops.domain.dto.ProfileRequest;
import org.folio.bulkops.domain.dto.ProfileSummaryDTO;
import org.folio.bulkops.domain.dto.ProfileDto;
import org.mapstruct.*;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, injectionStrategy = InjectionStrategy.CONSTRUCTOR,
  uses = {MappingMethods.class}, builder = @Builder(disableBuilder = true))
public interface ProfileMapper {
  @Mapping(target = "id", source = "profile.id")
  @Mapping(target = "name", source = "profile.name")
  @Mapping(target = "description", source = "profile.description")
  @Mapping(target = "locked", source = "profile.locked")
  @Mapping(target = "entityType", source = "profile.entityType")
  @Mapping(target = "bulkOperationRuleCollection", source = "profile.bulkOperationRuleCollection")
  @Mapping(target = "bulkOperationMarcRuleCollection", source = "profile.bulkOperationMarcRuleCollection")
//  @Mapping(target = "bulkOperationRuleCollection", source = "profile.bulkOperationRuleCollection", qualifiedByName = "mapToBulkOperationRuleCollection")
//  @Mapping(target = "bulkOperationMarcRuleCollection", source = "profile.bulkOperationMarcRuleCollection", qualifiedByName = "mapToBulkOperationMarcRuleCollection")
  @Mapping(target = "createdDate", source = "profile.createdDate")
  @Mapping(target = "createdBy", source = "profile.createdBy")
  @Mapping(target = "createdByUser", source = "profile.createdByUser")
  @Mapping(target = "updatedDate", source = "profile.updatedDate")
  @Mapping(target = "updatedBy", source = "profile.updatedBy")
  @Mapping(target = "updatedByUser", source = "profile.updatedByUser")
  ProfileSummaryDTO toSummmaryDTO(Profile profile);
  ProfileDto toDto(org.folio.bulkops.domain.entity.Profile entity);

  Profile toEntity(ProfileRequest dto);

  }






