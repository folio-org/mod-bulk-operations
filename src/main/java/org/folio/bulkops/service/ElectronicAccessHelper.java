package org.folio.bulkops.service;

import org.folio.bulkops.domain.bean.ElectronicAccess;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@RequiredArgsConstructor
@Log4j2
public class ElectronicAccessHelper implements InitializingBean {
  private final ElectronicAccessService electronicAccessService;

  public String electronicAccessToString(ElectronicAccess access) {
    return electronicAccessService.electronicAccessToString(access);
  }

  public String electronicAccessInstanceToString(ElectronicAccess access) {
    return electronicAccessService.electronicAccessInstanceToString(access);
  }

  public String itemElectronicAccessToString(ElectronicAccess access) {
    return electronicAccessService.itemElectronicAccessToString(access);
  }

  public ElectronicAccess restoreElectronicAccessItem(String electronicAccess) {
    return electronicAccessService.restoreElectronicAccessItem(electronicAccess);
  }

  public ElectronicAccess restoreItemElectronicAccessItem(String electronicAccess) {
    return electronicAccessService.restoreItemElectronicAccessItem(electronicAccess);
  }

  private static ElectronicAccessHelper service;

  @Override
  public void afterPropertiesSet() {
    service = this;
  }

  public static ElectronicAccessHelper service() {
    return service;
  }
}
