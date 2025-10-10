package org.folio.bulkops.service;

import static java.lang.String.format;
import static org.folio.bulkops.util.Constants.QUERY_PATTERN_NAME;
import static org.folio.bulkops.util.FolioExecutionContextUtil.prepareContextForTenant;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.bulkops.client.ElectronicAccessRelationshipClient;
import org.folio.bulkops.exception.NotFoundException;
import org.folio.bulkops.exception.ReferenceDataNotFoundException;
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
  private final LocalReferenceDataService localReferenceDataService;

  @Cacheable(cacheNames = "electronicAccessRelationshipNames")
  public String getRelationshipNameById(String id) {
    var tenantId = localReferenceDataService.getTenantByUrlRelationshipId(id);
    log.info("getRelationshipNameById: {}, {}", id, tenantId);
    try (var ignored = new FolioExecutionContextSetter(prepareContextForTenant(tenantId,
            folioModuleMetadata, folioExecutionContext))) {
      return relationshipClient.getById(id).getName();
    } catch (NotFoundException e) {
      var msg = format("Electronic access relationship not found by id=%s", id);
      log.error(msg);
      throw new ReferenceDataNotFoundException(msg);
    }
  }

  @Cacheable(cacheNames = "electronicAccessRelationshipIds")
  public String getRelationshipIdByName(String name) {
    var relationShips = relationshipClient.getByQuery(String.format(QUERY_PATTERN_NAME, name));
    return relationShips.getElectronicAccessRelationships().isEmpty()
            ? name : relationShips.getElectronicAccessRelationships().getFirst().getId();
  }
}
