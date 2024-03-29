package org.folio.bulkops.processor;

import org.folio.bulkops.client.UserClient;
import org.folio.bulkops.domain.bean.User;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class UserUpdateProcessor extends AbstractUpdateProcessor<User> {
  private final UserClient userClient;

  @Override
  public void updateRecord(User user) {
    userClient.updateUser(user, user.getId());
  }

  @Override
  public Class<User> getUpdatedType() {
    return User.class;
  }
}
