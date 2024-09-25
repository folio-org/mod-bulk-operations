package org.folio.bulkops.processor;

import org.folio.bulkops.client.UserClient;
import org.folio.bulkops.domain.bean.User;
import org.folio.bulkops.domain.dto.EntityType;
import org.folio.bulkops.processor.check.PermissionsValidator;
import org.folio.spring.FolioExecutionContext;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class UserUpdateProcessor extends AbstractUpdateProcessor<User> {

  private static final String NO_USER_WRITE_PERMISSIONS_TEMPLATE = "User %s does not have required permission to edit the user record - %s=%s on the tenant ";

  private final UserClient userClient;
  private final PermissionsValidator permissionsValidator;
  private final FolioExecutionContext folioExecutionContext;

  @Override
  public void updateRecord(User user) {
    permissionsValidator.checkIfBulkEditWritePermissionExists(folioExecutionContext.getTenantId(), EntityType.ITEM,
      NO_USER_WRITE_PERMISSIONS_TEMPLATE + folioExecutionContext.getTenantId());
    userClient.updateUser(user, user.getId());
  }

  @Override
  public Class<User> getUpdatedType() {
    return User.class;
  }
}
