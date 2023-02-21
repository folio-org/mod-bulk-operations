package org.folio.bulkops.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Log4j2
public class HoldingsReferenceHelper implements InitializingBean {

  private final HoldingsReferenceService holdingsReferenceService;

  public String getInstanceTitleById(String id) {
    return holdingsReferenceService.getInstanceTitleById(id);
  }

  public String getHoldingsTypeNameById(String id) {
    return holdingsReferenceService.getHoldingsTypeNameById(id);
  }

  public String getHoldingsTypeIdByName(String name) {
    return holdingsReferenceService.getHoldingsTypeIdByName(name);
  }

  public String getLocationNameById(String id) {
    return holdingsReferenceService.getLocationNameById(id);
  }

  public String getLocationIdByName(String name) {
    return holdingsReferenceService.getLocationIdByName(name);
  }

  public String getCallNumberTypeNameById(String id) {
    return holdingsReferenceService.getCallNumberTypeNameById(id);
  }

  public String getCallNumberTypeIdByName(String name) {
    return holdingsReferenceService.getCallNumberTypeIdByName(name);
  }

  public String getNoteTypeNameById(String id) {
    return holdingsReferenceService.getNoteTypeNameById(id);
  }

  public String getNoteTypeIdByName(String name) {
    return holdingsReferenceService.getNoteTypeIdByName(name);
  }

  public String getIllPolicyNameById(String id) {
    return holdingsReferenceService.getIllPolicyNameById(id);
  }

  public String getIllPolicyIdByName(String name) {
    return holdingsReferenceService.getIllPolicyIdByName(name);
  }

  public String getSourceNameById(String id) {
    return holdingsReferenceService.getSourceNameById(id);
  }

  public String getSourceIdByName(String name) {
    return holdingsReferenceService.getSourceIdByName(name);
  }

  public String getStatisticalCodeNameById(String id) {
    return holdingsReferenceService.getStatisticalCodeNameById(id);
  }

  public String getStatisticalCodeIdByName(String name) {
    return holdingsReferenceService.getStatisticalCodeIdByName(name);
  }

  private static HoldingsReferenceHelper service;

  @Override
  public void afterPropertiesSet() throws Exception {
    service = this;
  }

  public static HoldingsReferenceHelper service() {
    return service;
  }
}
