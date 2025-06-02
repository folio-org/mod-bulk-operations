package org.folio.bulkops.mapper;

import org.folio.bulkops.domain.entity.Profile;
import org.folio.bulkops.domain.dto.ProfileSummaryDto;
import org.mapstruct.*;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, injectionStrategy = InjectionStrategy.CONSTRUCTOR,
  uses = {ProfileMapper.class}, builder = @Builder(disableBuilder = true))
public interface ProfileMapper {
  ProfileSummaryDto toSummaryDto(Profile profile);
}
