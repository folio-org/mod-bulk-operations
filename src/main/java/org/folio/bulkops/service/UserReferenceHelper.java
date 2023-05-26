package org.folio.bulkops.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.bulkops.domain.bean.AddressType;
import org.folio.bulkops.domain.bean.CustomField;
import org.folio.bulkops.domain.bean.Department;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Log4j2
public class UserReferenceHelper implements InitializingBean {
  private final UserReferenceService userReferenceService;

  public AddressType getAddressTypeByDesc(String desc) {
    return userReferenceService.getAddressTypeByDesc(desc);
  }

  public AddressType getAddressTypeById(String id) {
    return userReferenceService.getAddressTypeById(id);
  }

  public Department getDepartmentById(String id) {
    return userReferenceService.getDepartmentById(id);
  }

  public Department getDepartmentByName(String name) {
    return userReferenceService.getDepartmentByName(name);
  }

  public String getPatronGroupNameById(String id) {
    return userReferenceService.getPatronGroupNameById(id);
  }

  public String getPatronGroupIdByName(String name) {
    return userReferenceService.getPatronGroupIdByName(name);
  }

  public CustomField getCustomFieldByRefId(String refId) {
    return userReferenceService.getCustomFieldByRefId(refId);
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
