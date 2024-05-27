package org.folio.bulkops.mapper;

import java.util.UUID;
import org.folio.bulkops.domain.bean.Personal;
import org.folio.bulkops.domain.bean.User;
import org.springframework.stereotype.Component;

@Component
public class UserToUserDtoMapperImpl implements UserToUserDtoMapper {

  @Override
  public org.folio.bulkops.domain.dto.User mapUserToUserDto(User user) {
    Personal userPersonal = user.getPersonal();
    return new org.folio.bulkops.domain.dto.User()
      .id(UUID.fromString(user.getId()))
      .firstName(user.getPersonal().getFirstName())
      .lastName(userPersonal.getLastName())
      .preferredFirstName(user.getPersonal().getPreferredFirstName())
      .middleName(userPersonal.getMiddleName());
  }
}
