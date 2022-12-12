package org.folio.bulkops.processor;

import lombok.RequiredArgsConstructor;
import org.folio.bulkops.client.UserClient;
import org.folio.bulkops.domain.dto.User;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserUpdateProcessor implements UpdateProcessor<User> {
  private final UserClient userClient;

  @Override
  public void updateRecord(User user) {
    userClient.updateUser(user, user.getId());
  }
}