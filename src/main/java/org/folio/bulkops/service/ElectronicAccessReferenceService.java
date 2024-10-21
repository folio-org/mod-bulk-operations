package org.folio.bulkops.service;

import static java.util.Objects.isNull;
import static org.folio.bulkops.util.Constants.QUERY_PATTERN_NAME;
import static org.folio.bulkops.util.FolioExecutionContextUtil.prepareContextForTenant;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.bulkops.client.ElectronicAccessRelationshipClient;
import org.folio.bulkops.exception.NotFoundException;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.FolioModuleMetadata;
import org.folio.spring.scope.FolioExecutionContextSetter;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Log4j2
public class ElectronicAccessReferenceService {
  private final ElectronicAccessRelationshipClient relationshipClient;
  private final FolioExecutionContext folioExecutionContext;
  private final FolioModuleMetadata folioModuleMetadata;

  @Cacheable(cacheNames = "electronicAccessRelationshipNames")
  public String getRelationshipNameById(String id, String tenantId) {
    log.info("getRelationshipNameById: {}, {}", id, tenantId);
    try {
      return relationshipClient.getById(id).getName();
    } catch (NotFoundException e) {
      log.error("Electronic access relationship was not found by id={}", id);
      return id;
    }
  }

  @Cacheable(cacheNames = "electronicAccessRelationshipIds")
  public String getRelationshipIdByName(String name) {
    var relationShips = relationshipClient.getByQuery(String.format(QUERY_PATTERN_NAME, name));
    return relationShips.getElectronicAccessRelationships().isEmpty() ?
      name :
      relationShips.getElectronicAccessRelationships().get(0).getId();
  }
}
