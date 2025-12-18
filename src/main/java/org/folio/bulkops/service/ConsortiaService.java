package org.folio.bulkops.service;

import static org.folio.spring.utils.FolioExecutionContextUtils.prepareContextForTenant;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.folio.bulkops.client.ConsortiaClient;
import org.folio.bulkops.client.ConsortiumClient;
import org.folio.bulkops.domain.bean.UserTenant;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.FolioModuleMetadata;
import org.folio.spring.scope.FolioExecutionContextSetter;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Log4j2
public class ConsortiaService {

  private final FolioExecutionContext context;
  private final FolioModuleMetadata folioModuleMetadata;
  private final ConsortiaClient consortiaClient;
  private final ConsortiumClient consortiumClient;

  @Cacheable(value = "centralTenantCache")
  public String getCentralTenantId(String currentTenantId) {
    var userTenantCollection = consortiaClient.getUserTenantCollection();
    var userTenants = userTenantCollection.getUserTenants();
    if (!userTenants.isEmpty()) {
      log.info("userTenants: {}", userTenants);
      return userTenants.getFirst().getCentralTenantId();
    }
    log.info("No central tenant found for {}", currentTenantId);
    return StringUtils.EMPTY;
  }

  @Cacheable(value = "affiliatedTenantsCache")
  public List<String> getAffiliatedTenants(String currentTenantId, String userId) {
    var consortia = consortiumClient.getConsortia();
    var consortiaList = consortia.getConsortia();
    if (!consortiaList.isEmpty()) {
      var userTenants =
          consortiumClient.getConsortiaUserTenants(
              consortiaList.getFirst().getId(), userId, Integer.MAX_VALUE);
      return userTenants.getUserTenants().stream().map(UserTenant::getTenantId).toList();
    }
    return new ArrayList<>();
  }

  @Cacheable(value = "getUserTenantsPerIdCache")
  public Map<String, UserTenant> getUserTenantsPerId(String currentTenantId, String userId) {
    var centralTenantId = getCentralTenantId(currentTenantId);
    if (StringUtils.isNotEmpty(centralTenantId)) {
      try (var ignored =
          new FolioExecutionContextSetter(
              prepareContextForTenant(centralTenantId, folioModuleMetadata, context))) {
        var consortia = consortiumClient.getConsortia();
        var consortiaList = consortia.getConsortia();
        if (!consortiaList.isEmpty()) {
          var userTenants =
              consortiumClient.getConsortiaUserTenants(
                  consortiaList.getFirst().getId(), userId, Integer.MAX_VALUE);
          return userTenants.getUserTenants().stream()
              .collect(Collectors.toMap(UserTenant::getTenantId, userTenant -> userTenant));
        }
      }
    }
    return new HashMap<>();
  }

  @Cacheable(value = "isTenantCentral")
  public boolean isTenantCentral(String tenantId) {
    return tenantId.equals(getCentralTenantId(tenantId));
  }

  @Cacheable(value = "isTenantMember")
  public boolean isTenantMember(String tenantId) {
    return isTenantInConsortia(tenantId) && !isTenantCentral(tenantId);
  }

  @Cacheable(value = "isTenantInConsortia")
  public boolean isTenantInConsortia(String tenantId) {
    return StringUtils.isNotEmpty(getCentralTenantId(tenantId));
  }
}
