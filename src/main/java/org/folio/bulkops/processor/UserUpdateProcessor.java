package org.folio.bulkops.processor;

import org.folio.bulkops.client.UserClient;
import org.folio.bulkops.domain.bean.User;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class UserUpdateProcessor implements UpdateProcessor<User> {
  private final UserClient userClient;

  @Override
  public void updateRecord(User user, UUID operationId) {
    userClient.updateUser(user, user.getId());
  }

  @Override
  public Class<User> getUpdatedType() {
    return User.class;
  }
}
