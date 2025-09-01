package org.folio.bulkops.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.bulkops.client.SubjectSourcesClient;
import org.folio.bulkops.client.SubjectTypesClient;
import org.folio.bulkops.exception.NotFoundException;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.FolioModuleMetadata;
import org.folio.spring.scope.FolioExecutionContextSetter;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import static java.util.Objects.isNull;
import static org.folio.bulkops.util.Constants.QUERY_PATTERN_NAME;
import static org.folio.bulkops.util.FolioExecutionContextUtil.prepareContextForTenant;

@Service
@RequiredArgsConstructor
@Log4j2
public class SubjectReferenceService {

  private final FolioExecutionContext folioExecutionContext;
  private final FolioModuleMetadata folioModuleMetadata;
  private final SubjectSourcesClient subjectSourcesClient;
  private final SubjectTypesClient subjectTypesClient;

  @Cacheable(cacheNames = "subjectSourceNames")
  public String getSubjectSourceNameById(String id, String tenantId) {
    log.info("getSubjectSourceNameById: {}, {}", id, tenantId);
    if (isNull(tenantId)) {
      tenantId = folioExecutionContext.getTenantId();
    }
    try (var ignored = new FolioExecutionContextSetter(prepareContextForTenant(tenantId, folioModuleMetadata, folioExecutionContext))) {
      return subjectSourcesClient.getById(id).getName();
    } catch (NotFoundException e) {
      log.error("Subject source was not found by id={}", id);
      return id;
    }
  }

  @Cacheable(cacheNames = "subjectTypeNames")
  public String getSubjectTypeNameById(String id, String tenantId) {
    log.info("getSubjectTypeNameById: {}, {}", id, tenantId);
    if (isNull(tenantId)) {
      tenantId = folioExecutionContext.getTenantId();
    }
    try (var ignored = new FolioExecutionContextSetter(prepareContextForTenant(tenantId, folioModuleMetadata, folioExecutionContext))) {
      return subjectTypesClient.getById(id).getName();
    } catch (NotFoundException e) {
      log.error("Subject type was not found by id={}", id);
      return id;
    }
  }

  @Cacheable(cacheNames = "subjectSourceIds")
  public String getSubjectSourceIdByName(String name) {
    var subjectSources = subjectSourcesClient.getByQuery(String.format(QUERY_PATTERN_NAME, name));
    return subjectSources.getSubjectSources().isEmpty() ?
      name :
      subjectSources.getSubjectSources().get(0).getId();
  }

  @Cacheable(cacheNames = "subjectSourceExists")
  public boolean subjectSourceExists(String name) {
    var subjectSources = subjectSourcesClient.getByQuery(String.format(QUERY_PATTERN_NAME, name));
    return !subjectSources.getSubjectSources().isEmpty();
  }

  @Cacheable(cacheNames = "subjectTypeIds")
  public String getSubjectTypeIdByName(String name) {
    var subjectTypes = subjectTypesClient.getByQuery(String.format(QUERY_PATTERN_NAME, name));
    return subjectTypes.getSubjectTypes().isEmpty() ?
      name :
      subjectTypes.getSubjectTypes().get(0).getId();
  }
}
