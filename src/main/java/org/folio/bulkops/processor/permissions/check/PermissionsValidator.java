package org.folio.bulkops.processor.permissions.check;

import static org.folio.bulkops.domain.dto.EntityType.HOLDINGS_RECORD;
import static org.folio.bulkops.domain.dto.EntityType.INSTANCE;
import static org.folio.bulkops.domain.dto.EntityType.INSTANCE_MARC;
import static org.folio.bulkops.domain.dto.EntityType.ITEM;
import static org.folio.bulkops.domain.dto.EntityType.USER;
import static org.folio.bulkops.processor.permissions.check.PermissionEnum.BULK_EDIT_INVENTORY_VIEW_PERMISSION;
import static org.folio.bulkops.processor.permissions.check.PermissionEnum.BULK_EDIT_INVENTORY_WRITE_PERMISSION;
import static org.folio.bulkops.processor.permissions.check.PermissionEnum.BULK_EDIT_USERS_VIEW_PERMISSION;
import static org.folio.bulkops.processor.permissions.check.PermissionEnum.BULK_EDIT_USERS_WRITE_PERMISSION;
import static org.folio.bulkops.processor.permissions.check.PermissionEnum.BULK_OPERATIONS_PROFILES_ITEM_LOCK;
import static org.folio.bulkops.util.Constants.DUPLICATES_ACROSS_TENANTS;
import static org.folio.bulkops.util.Constants.LINKED_DATA_SOURCE;
import static org.folio.bulkops.util.Constants.LINKED_DATA_SOURCE_IS_NOT_SUPPORTED;
import static org.folio.bulkops.util.Constants.NO_HOLDING_VIEW_PERMISSIONS;
import static org.folio.bulkops.util.Constants.NO_INSTANCE_VIEW_PERMISSIONS;
import static org.folio.bulkops.util.Constants.NO_ITEM_VIEW_PERMISSIONS;
import static org.folio.bulkops.util.Constants.NO_MATCH_FOUND_MESSAGE;
import static org.folio.bulkops.util.Constants.NO_USER_VIEW_PERMISSIONS;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.folio.bulkops.client.SearchConsortium;
import org.folio.bulkops.client.UserClient;
import org.folio.bulkops.domain.bean.BulkOperationsEntity;
import org.folio.bulkops.domain.bean.ConsortiumHolding;
import org.folio.bulkops.domain.bean.ConsortiumItem;
import org.folio.bulkops.domain.bean.User;
import org.folio.bulkops.domain.dto.EntityType;
import org.folio.bulkops.domain.entity.BulkOperation;
import org.folio.bulkops.exception.ProfileLockedException;
import org.folio.bulkops.exception.ReadPermissionException;
import org.folio.bulkops.exception.UploadFromQueryException;
import org.folio.bulkops.exception.WritePermissionDoesNotExist;
import org.folio.bulkops.service.ConsortiaService;
import org.folio.spring.FolioExecutionContext;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PermissionsValidator {

  private final PermissionsProvider permissionsProvider;
  private final RequiredPermissionResolver requiredPermissionResolver;
  private final FolioExecutionContext folioExecutionContext;
  private final UserClient userClient;
  private final ConsortiaService consortiaService;
  private final TenantResolver tenantResolver;
  private final SearchConsortium searchClient;
  private final ReadPermissionsValidator readPermissionsValidator;

  public boolean isBulkEditReadPermissionExists(String tenantId, EntityType entityType) {
    var readPermissionForEntity = requiredPermissionResolver.getReadPermission(entityType);
    var userPermissions =
        permissionsProvider.getUserPermissions(tenantId, folioExecutionContext.getUserId());
    var isReadPermissionsExist = false;
    if (entityType == EntityType.USER) {
      isReadPermissionsExist =
          userPermissions.contains(readPermissionForEntity.getValue())
              && userPermissions.contains(BULK_EDIT_USERS_VIEW_PERMISSION.getValue());
    } else {
      isReadPermissionsExist =
          userPermissions.contains(readPermissionForEntity.getValue())
              && userPermissions.contains(BULK_EDIT_INVENTORY_VIEW_PERMISSION.getValue());
    }
    return isReadPermissionsExist;
  }

  public void checkIfBulkEditWritePermissionExists(
      String tenantId, EntityType entityType, String errorMessage) {
    if (!isBulkEditWritePermissionExists(tenantId, entityType)) {
      throw new WritePermissionDoesNotExist(errorMessage);
    }
  }

  public void checkPermissions(BulkOperation operation, BulkOperationsEntity entityRecord)
      throws UploadFromQueryException {
    if (Set.of(USER, INSTANCE, INSTANCE_MARC).contains(operation.getEntityType())) {
      if (!readPermissionsValidator.isBulkEditReadPermissionExists(
          folioExecutionContext.getTenantId(), operation.getEntityType())) {
        var user = userClient.getUserById(folioExecutionContext.getUserId().toString());
        throw new ReadPermissionException(
            buildReadPermissionErrorMessage(operation, entityRecord.getId(), user),
            entityRecord.getId());
      }
      if (LINKED_DATA_SOURCE.equals(entityRecord.getSource())) {
        throw new UploadFromQueryException(
            LINKED_DATA_SOURCE_IS_NOT_SUPPORTED, entityRecord.getId());
      }
    } else if (HOLDINGS_RECORD == operation.getEntityType()) {
      checkPermissionsAndAffiliationsForHoldings(entityRecord.getId());
    } else if (ITEM == operation.getEntityType()) {
      checkPermissionsAndAffiliationsForItem(entityRecord.getId());
    }
  }

  public void checkIfLockPermissionExists() {
    List<String> userPermissions =
        permissionsProvider.getUserPermissions(
            folioExecutionContext.getTenantId(), folioExecutionContext.getUserId());
    if (!userPermissions.contains(BULK_OPERATIONS_PROFILES_ITEM_LOCK.getValue())) {
      throw new ProfileLockedException("User is restricted to manage locked profiles");
    }
  }

  private boolean isBulkEditWritePermissionExists(String tenantId, EntityType entityType) {
    var writePermissionForEntity = requiredPermissionResolver.getWritePermission(entityType);
    var userPermissions =
        permissionsProvider.getUserPermissions(tenantId, folioExecutionContext.getUserId());
    var isWritePermissionsExist = false;
    if (entityType == EntityType.USER) {
      isWritePermissionsExist =
          userPermissions.contains(writePermissionForEntity.getValue())
              && userPermissions.contains(BULK_EDIT_USERS_WRITE_PERMISSION.getValue());
    } else {
      isWritePermissionsExist =
          userPermissions.contains(writePermissionForEntity.getValue())
              && userPermissions.contains(BULK_EDIT_INVENTORY_WRITE_PERMISSION.getValue());
    }
    return isWritePermissionsExist;
  }

  private String buildReadPermissionErrorMessage(
      BulkOperation operation, String identifier, User user) {
    return switch (operation.getEntityType()) {
      case USER ->
          NO_USER_VIEW_PERMISSIONS.formatted(
              user.getUsername(), "id", identifier, folioExecutionContext.getTenantId());
      case INSTANCE, INSTANCE_MARC ->
          NO_INSTANCE_VIEW_PERMISSIONS.formatted(
              user.getUsername(), "id", identifier, folioExecutionContext.getTenantId());
      default ->
          throw new IllegalArgumentException(
              "For %s this error message builder cannot be used."
                  .formatted(operation.getEntityType()));
    };
  }

  private void checkPermissionsAndAffiliationsForHoldings(String itemIdentifier)
      throws UploadFromQueryException {
    var centralTenantId = consortiaService.getCentralTenantId(folioExecutionContext.getTenantId());
    if (isCurrentTenantCentral(centralTenantId)) {
      // Process central tenant
      var consortiumHoldingsCollection =
          searchClient.getConsortiumHoldingCollection(
              new org.folio.bulkops.domain.dto.BatchIdsDto()
                  .identifierType(org.folio.bulkops.domain.dto.BatchIdsDto.IdentifierTypeEnum.ID)
                  .identifierValues(List.of(itemIdentifier)));
      if (!consortiumHoldingsCollection.getHoldings().isEmpty()) {
        var tenantIds =
            consortiumHoldingsCollection.getHoldings().stream()
                .map(ConsortiumHolding::getTenantId)
                .collect(Collectors.toSet());
        if (tenantIds.size() > 1) {
          throw new UploadFromQueryException(DUPLICATES_ACROSS_TENANTS, itemIdentifier);
        }
        tenantResolver.checkAffiliatedPermittedTenantIds(
            HOLDINGS_RECORD,
            org.folio.bulkops.domain.dto.IdentifierType.ID.getValue(),
            tenantIds,
            itemIdentifier);
      } else {
        throw new UploadFromQueryException(NO_MATCH_FOUND_MESSAGE, itemIdentifier);
      }
    } else {
      // Process local tenant case
      checkReadPermissions(
          folioExecutionContext.getTenantId(),
          itemIdentifier,
          HOLDINGS_RECORD,
          NO_HOLDING_VIEW_PERMISSIONS);
    }
  }

  private void checkPermissionsAndAffiliationsForItem(String itemIdentifier)
      throws UploadFromQueryException {
    var centralTenantId = consortiaService.getCentralTenantId(folioExecutionContext.getTenantId());
    if (isCurrentTenantCentral(centralTenantId)) {
      // Assuming item is requested by only one identifier not a collection of identifiers
      var batchIdsDto =
          new org.folio.bulkops.domain.dto.BatchIdsDto()
              .identifierType(org.folio.bulkops.domain.dto.BatchIdsDto.IdentifierTypeEnum.ID)
              .identifierValues(List.of(itemIdentifier));
      var consortiumItemCollection = searchClient.getConsortiumItemCollection(batchIdsDto);
      if (!consortiumItemCollection.getItems().isEmpty()) {
        var tenantIds =
            consortiumItemCollection.getItems().stream()
                .map(ConsortiumItem::getTenantId)
                .collect(Collectors.toSet());
        if (tenantIds.size() > 1) {
          throw new UploadFromQueryException(DUPLICATES_ACROSS_TENANTS, itemIdentifier);
        }
        tenantResolver.checkAffiliatedPermittedTenantIds(
            EntityType.ITEM,
            org.folio.bulkops.domain.dto.IdentifierType.ID.getValue(),
            tenantIds,
            itemIdentifier);
      } else {
        throw new UploadFromQueryException(NO_MATCH_FOUND_MESSAGE, itemIdentifier);
      }
    } else {
      // Process local tenant case
      checkReadPermissions(
          folioExecutionContext.getTenantId(), itemIdentifier, ITEM, NO_ITEM_VIEW_PERMISSIONS);
    }
  }

  private void checkReadPermissions(
      String tenantId, String identifier, EntityType entityType, String errorTemplate)
      throws ReadPermissionException {
    if (!readPermissionsValidator.isBulkEditReadPermissionExists(tenantId, entityType)) {
      var user = userClient.getUserById(folioExecutionContext.getUserId().toString());
      throw new ReadPermissionException(
          errorTemplate.formatted(
              user.getUsername(),
              org.folio.bulkops.domain.dto.IdentifierType.ID,
              identifier,
              tenantId),
          identifier);
    }
  }

  private boolean isCurrentTenantCentral(String centralTenantId) {
    return StringUtils.isNotEmpty(centralTenantId)
        && centralTenantId.equals(folioExecutionContext.getTenantId());
  }
}
