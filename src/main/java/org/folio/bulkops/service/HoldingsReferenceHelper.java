package org.folio.bulkops.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.bulkops.domain.bean.HoldingsRecordsSource;
import org.folio.bulkops.domain.bean.HoldingsType;
import org.folio.bulkops.domain.bean.IllPolicy;
import org.folio.bulkops.domain.bean.ItemLocation;
import org.folio.bulkops.domain.bean.StatisticalCode;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.FolioModuleMetadata;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Log4j2
public class HoldingsReferenceHelper implements InitializingBean {

  private final HoldingsReferenceService holdingsReferenceService;
  private final FolioExecutionContext folioExecutionContext;
  private final FolioModuleMetadata folioModuleMetadata;

  public HoldingsType getHoldingsTypeById(String id) {
    return holdingsReferenceService.getHoldingsTypeById(id);
  }

  public HoldingsType getHoldingsTypeByName(String name) {
    return holdingsReferenceService.getHoldingsTypeByName(name, folioExecutionContext.getTenantId());
  }

  public ItemLocation getLocationById(String id) {
    return holdingsReferenceService.getLocationById(id);
  }

  public ItemLocation getLocationByName(String name) {
    return holdingsReferenceService.getLocationIdByName(name);
  }

  public String getCallNumberTypeNameById(String id) {
    return holdingsReferenceService.getCallNumberTypeNameById(id);
  }

  public String getCallNumberTypeIdByName(String name) {
    return holdingsReferenceService.getCallNumberTypeIdByName(name, folioExecutionContext.getTenantId());
  }

  public String getNoteTypeNameById(String id, String tenantId) {
    return holdingsReferenceService.getNoteTypeNameById(id, tenantId);
  }

  public String getNoteTypeIdByName(String name) {
    return holdingsReferenceService.getNoteTypeIdByName(name, folioExecutionContext.getTenantId());
  }

  public IllPolicy getIllPolicyNameById(String id) {
    return holdingsReferenceService.getIllPolicyById(id);
  }

  public IllPolicy getIllPolicyByName(String name) {
    return holdingsReferenceService.getIllPolicyByName(name, folioExecutionContext.getTenantId());
  }

  public HoldingsRecordsSource getSourceById(String id) {
    return holdingsReferenceService.getSourceById(id);
  }

  public HoldingsRecordsSource getSourceByName(String name) {
    return holdingsReferenceService.getSourceByName(name, folioExecutionContext.getTenantId());
  }

  public StatisticalCode getStatisticalCodeById(String id) {
    return holdingsReferenceService.getStatisticalCodeById(id);
  }

  public StatisticalCode getStatisticalCodeByName(String name) {
    return holdingsReferenceService.getStatisticalCodeByName(name, folioExecutionContext.getTenantId());
  }

  private static HoldingsReferenceHelper service;

  @Override
  public void afterPropertiesSet() {
    service = this;
  }

  public static HoldingsReferenceHelper service() {
    return service;
  }
}
