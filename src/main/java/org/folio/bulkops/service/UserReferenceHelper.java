package org.folio.bulkops.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.bulkops.domain.bean.CustomField;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Log4j2
public class UserReferenceHelper implements InitializingBean {
  private final UserReferenceService userReferenceService;

  public String getAddressTypeIdByDesc(String desc) {
    return userReferenceService.getAddressTypeIdByDesc(desc);
  }

  public String getAddressTypeDescById(String id) {
      return userReferenceService.getAddressTypeDescById(id);
  }

  public String getDepartmentNameById(String id) {
    return userReferenceService.getDepartmentNameById(id);
  }

  public String getDepartmentIdByName(String name) {
    return userReferenceService.getDepartmentIdByName(name);
  }

  public String getPatronGroupNameById(String id) {
    return userReferenceService.getPatronGroupNameById(id);
  }

  public String getPatronGroupIdByName(String name) {
    return userReferenceService.getPatronGroupIdByName(name);
  }

  public CustomField getCustomFieldByName(String name)  {
    return userReferenceService.getCustomFieldByName(name);
  }

  private static UserReferenceHelper service = null;

  @Override
  public void afterPropertiesSet() {
    service = this;
  }

  public static UserReferenceHelper service() {
    return service;
  }
}
