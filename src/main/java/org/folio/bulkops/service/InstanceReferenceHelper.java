package org.folio.bulkops.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.bulkops.domain.bean.StatisticalCode;
import org.folio.spring.FolioExecutionContext;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;

@Service
@Log4j2
@RequiredArgsConstructor
public class InstanceReferenceHelper implements InitializingBean {
  private final InstanceReferenceService instanceReferenceService;
  private final FolioExecutionContext folioExecutionContext;

  public String getInstanceStatusNameById(String id) {
    return instanceReferenceService.getInstanceStatusNameById(id);
  }

  public String getInstanceStatusIdByName(String name) {
    return instanceReferenceService.getInstanceStatusIdByName(name);
  }

  public String getModeOfIssuanceNameById(String id) {
    return instanceReferenceService.getModeOfIssuanceNameById(id);
  }

  public String getModeOfIssuanceIdByName(String name) {
    return instanceReferenceService.getModeOfIssuanceIdByName(name);
  }

  public StatisticalCode getStatisticalCodeByName(String name) {
    return instanceReferenceService.getStatisticalCodeByName(name, folioExecutionContext.getTenantId());
  }

  public StatisticalCode getStatisticalCodeById(String id) {
    return instanceReferenceService.getStatisticalCodeById(id, folioExecutionContext.getTenantId());
  }

  public String getInstanceTypeNameById(String id) {
    return instanceReferenceService.getInstanceTypeNameById(id);
  }

  public String getInstanceTypeIdByName(String name) {
    return instanceReferenceService.getInstanceTypeIdByName(name);
  }

  public String getNatureOfContentTermNameById(String id) {
    return instanceReferenceService.getNatureOfContentTermNameById(id);
  }

  public String getNatureOfContentTermIdByName(String name) {
    return instanceReferenceService.getNatureOfContentTermIdByName(name);
  }

  public String getInstanceFormatNameById(String id) {
    return instanceReferenceService.getInstanceFormatNameById(id);
  }

  public String getInstanceFormatIdByName(String name) {
    return instanceReferenceService.getInstanceFormatIdByName(name);
  }

  public String getNoteTypeNameById(String id) {
    return instanceReferenceService.getNoteTypeNameById(id);
  }

  public String getNoteTypeIdByName(String name) {
    return instanceReferenceService.getNoteTypeIdByName(name);
  }

  private static InstanceReferenceHelper service;

  @Override
  public void afterPropertiesSet() {
    service = this;
  }

  public static InstanceReferenceHelper service() {
    return service;
  }
}
