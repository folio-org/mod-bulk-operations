package org.folio.bulkops.service;

import org.apache.commons.lang3.StringUtils;
import org.folio.bulkops.client.ConsortiaClient;
import org.folio.bulkops.client.ConsortiumClient;
import org.folio.bulkops.domain.bean.UserTenant;
import org.folio.spring.FolioExecutionContext;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Log4j2
public class ConsortiaService {

  private final FolioExecutionContext context;

  private final ConsortiaClient consortiaClient;
  private final ConsortiumClient consortiumClient;

  @Cacheable(value = "centralTenantCache")
  public String getCentralTenantId(String currentTenantId) {
    var userTenantCollection = consortiaClient.getUserTenantCollection();
    var userTenants = userTenantCollection.getUserTenants();
    if (!userTenants.isEmpty()) {
      log.info("userTenants: {}", userTenants);
      return userTenants.get(0).getCentralTenantId();
    }
    log.info("No central tenant found for {}", currentTenantId);
    return StringUtils.EMPTY;
  }

  @Cacheable(value = "affiliatedTenantsCache")
  public List<String> getAffiliatedTenants(String currentTenantId, String userId) {
    var consortia = consortiumClient.getConsortia();
    var consortiaList = consortia.getConsortia();
    if (!consortiaList.isEmpty()) {
      var userTenants = consortiumClient.getConsortiaUserTenants(consortiaList.get(0).getId(), userId, Integer.MAX_VALUE);
      return userTenants.getUserTenants().stream().map(UserTenant::getTenantId).toList();
    }
    return new ArrayList<>();
  }

  @Cacheable(value = "getUserTenantsPerIdCache")
  public Map<String, UserTenant> getUserTenantsPerId(String currentTenantId, String userId) {
    var consortia = consortiumClient.getConsortia();
    var consortiaList = consortia.getConsortia();
    if (!consortiaList.isEmpty()) {
      var userTenants = consortiumClient.getConsortiaUserTenants(consortiaList.get(0).getId(), userId, Integer.MAX_VALUE);
      return userTenants.getUserTenants().stream().collect(Collectors.toMap(UserTenant::getTenantId, userTenant -> userTenant));
    }
    return new HashMap<>();
  }

  @Cacheable(value = "isCurrentTenantCentralTenant")
  public boolean isCurrentTenantCentralTenant(String currentTenantId) {
    return getCentralTenantId(currentTenantId).equals(context.getTenantId());
  }

  @Cacheable(value = "isCurrentTenantInConsortia")
  public boolean isCurrentTenantInConsortia(String currentTenantId) {
    return StringUtils.isNotEmpty(getCentralTenantId(currentTenantId));
  }
}
