package org.folio.bulkops.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.bulkops.domain.bean.ElectronicAccess;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Log4j2
public class ElectronicAccessHelper implements InitializingBean {
  private final ElectronicAccessService electronicAccessService;

  public String electronicAccessToString(ElectronicAccess access) {
    return electronicAccessService.electronicAccessToString(access);
  }

  public ElectronicAccess restoreElectronicAccessItem(String s) {
    return electronicAccessService.restoreElectronicAccessItem(s);
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
