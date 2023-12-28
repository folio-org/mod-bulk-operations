package org.folio.bulkops.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;

@Service
@Log4j2
@RequiredArgsConstructor
public class InstanceReferenceHelper implements InitializingBean {
  private final InstanceReferenceService instanceReferenceService;

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

  private static InstanceReferenceHelper service;

  @Override
  public void afterPropertiesSet() {
    service = this;
  }

  public static InstanceReferenceHelper service() {
    return service;
  }
}
