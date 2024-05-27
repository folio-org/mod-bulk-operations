package org.folio.bulkops.mapper;

import org.folio.bulkops.domain.bean.User;

public interface UserToUserDtoMapper {
  org.folio.bulkops.domain.dto.User mapUserToUserDto(User user);
}
