package org.folio.bulkops.service;

import static org.folio.bulkops.processor.permissions.check.PermissionEnum.BULK_EDIT_INVENTORY_VIEW_PERMISSION;
import static org.folio.bulkops.processor.permissions.check.PermissionEnum.BULK_EDIT_INVENTORY_WRITE_PERMISSION;
import static org.folio.bulkops.processor.permissions.check.PermissionEnum.BULK_EDIT_USERS_VIEW_PERMISSION;
import static org.folio.bulkops.processor.permissions.check.PermissionEnum.BULK_EDIT_USERS_WRITE_PERMISSION;
import static org.folio.bulkops.processor.permissions.check.PermissionEnum.BULK_OPERATIONS_PROFILES_ITEM_LOCK;
import static org.folio.bulkops.processor.permissions.check.PermissionEnum.INVENTORY_INSTANCES_ITEM_GET_PERMISSION;
import static org.folio.bulkops.processor.permissions.check.PermissionEnum.INVENTORY_INSTANCES_ITEM_PUT;
import static org.folio.bulkops.processor.permissions.check.PermissionEnum.INVENTORY_ITEMS_ITEM_GET_PERMISSION;
import static org.folio.bulkops.processor.permissions.check.PermissionEnum.INVENTORY_ITEMS_ITEM_PUT;
import static org.folio.bulkops.processor.permissions.check.PermissionEnum.INVENTORY_STORAGE_HOLDINGS_ITEM_GET_PERMISSION;
import static org.folio.bulkops.processor.permissions.check.PermissionEnum.INVENTORY_STORAGE_HOLDINGS_ITEM_PUT;
import static org.folio.bulkops.processor.permissions.check.PermissionEnum.USERS_ITEM_PUT;
import static org.folio.bulkops.processor.permissions.check.PermissionEnum.USER_ITEM_GET_PERMISSION;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.folio.bulkops.client.EurekaUserPermissionsClient;
import org.folio.bulkops.client.OkapiUserPermissionsClient;
import org.folio.spring.FolioExecutionContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Log4j2
@Service
public class UserPermissionsService {

  public static final String EUREKA_PLATFORM = "eureka";
  public static final String OKAPI_PLATFORM = "okapi";

  @Setter
  @Value("${application.platform}")
  private String platform;

  private final FolioExecutionContext folioExecutionContext;
  private final OkapiUserPermissionsClient okapiUserPermissionsClient;
  private final EurekaUserPermissionsClient eurekaUserPermissionsClient;

  public List<String> getPermissions() {
    if (StringUtils.equals(EUREKA_PLATFORM, platform)) {
      var desiredPermissions = getDesiredPermissions();
      return eurekaUserPermissionsClient.getPermissions(
              folioExecutionContext.getUserId().toString(),
        desiredPermissions).getPermissions();
    }
    return okapiUserPermissionsClient.getPermissions(
            folioExecutionContext.getUserId().toString()).getPermissionNames();
  }

  private List<String> getDesiredPermissions() {
    return List.of(BULK_EDIT_INVENTORY_WRITE_PERMISSION.getValue(),
            BULK_EDIT_USERS_WRITE_PERMISSION.getValue(),
      USERS_ITEM_PUT.getValue(), INVENTORY_ITEMS_ITEM_PUT.getValue(),
            INVENTORY_STORAGE_HOLDINGS_ITEM_PUT.getValue(),
      INVENTORY_INSTANCES_ITEM_PUT.getValue(), BULK_EDIT_INVENTORY_VIEW_PERMISSION.getValue(),
            BULK_EDIT_USERS_VIEW_PERMISSION.getValue(),
      USER_ITEM_GET_PERMISSION.getValue(), INVENTORY_ITEMS_ITEM_GET_PERMISSION.getValue(),
      INVENTORY_INSTANCES_ITEM_GET_PERMISSION.getValue(),
            INVENTORY_STORAGE_HOLDINGS_ITEM_GET_PERMISSION.getValue(),
            BULK_OPERATIONS_PROFILES_ITEM_LOCK.getValue());
  }
}
