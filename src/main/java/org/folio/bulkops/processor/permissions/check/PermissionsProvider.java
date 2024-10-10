package org.folio.bulkops.processor.permissions.check;

import java.util.List;
import java.util.UUID;

import org.folio.bulkops.client.PermissionsSelfCheckClient;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.FolioModuleMetadata;
import org.folio.spring.scope.FolioExecutionContextSetter;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

import static org.folio.bulkops.util.FolioExecutionContextUtil.prepareContextForTenant;

@Component
@RequiredArgsConstructor
public class PermissionsProvider {

  private final PermissionsSelfCheckClient permissionsSelfCheckClient;
  private final FolioExecutionContext folioExecutionContext;
  private final FolioModuleMetadata folioModuleMetadata;

  @Cacheable(cacheNames = "userPermissions")
  public List<String> getUserPermissions(String tenantId, UUID userId) {
    try (var ignored =  new FolioExecutionContextSetter(prepareContextForTenant(tenantId, folioModuleMetadata, folioExecutionContext))) {
      return permissionsSelfCheckClient.getUserPermissionsForSelfCheck();
    }
  }
}
