package org.folio.bulkops.service;

import static java.util.Objects.isNull;
import static org.folio.bulkops.util.Constants.QUERY_PATTERN_NAME;
import static org.folio.bulkops.util.FolioExecutionContextUtil.prepareContextForTenant;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.bulkops.client.ClassificationTypesClient;
import org.folio.bulkops.exception.NotFoundException;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.FolioModuleMetadata;
import org.folio.spring.scope.FolioExecutionContextSetter;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Log4j2
public class ClassificationReferenceService {

  private final FolioExecutionContext folioExecutionContext;
  private final FolioModuleMetadata folioModuleMetadata;
  private final ClassificationTypesClient classificationTypesClient;

  @Cacheable(cacheNames = "classificationTypeNames")
  public String getClassificationTypeNameById(String id, String tenantId) {
    log.info("getClassificationTypeNameById: {}, {}", id, tenantId);
    if (isNull(tenantId)) {
      tenantId = folioExecutionContext.getTenantId();
    }
    try (var ignored = new FolioExecutionContextSetter(prepareContextForTenant(tenantId, folioModuleMetadata, folioExecutionContext))) {
      return classificationTypesClient.getById(id).getName();
    } catch (NotFoundException e) {
      log.error("Classification type was not found by id={}", id);
      return id;
    }
  }

  @Cacheable(cacheNames = "classificationTypeIds")
  public String getClassificationTypeIdByName(String name) {
    var classifications = classificationTypesClient.getByQuery(QUERY_PATTERN_NAME.formatted(name));
    return classifications.getClassificationTypes().isEmpty() ?
      name :
      classifications.getClassificationTypes().getFirst().getId();
  }
}
