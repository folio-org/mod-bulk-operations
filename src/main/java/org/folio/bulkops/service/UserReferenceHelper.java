package org.folio.bulkops.service;

import org.folio.bulkops.domain.bean.AddressType;
import org.folio.bulkops.domain.bean.CustomField;
import org.folio.bulkops.domain.bean.Department;
import org.folio.bulkops.domain.bean.PreferredContactType;
import org.folio.bulkops.domain.bean.UserGroup;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@RequiredArgsConstructor
@Log4j2
public class UserReferenceHelper implements InitializingBean {
  private final UserReferenceService userReferenceService;

  public AddressType getAddressTypeByAddressTypeValue(String addressTypeValue) {
    return userReferenceService.getAddressTypeByAddressTypeValue(addressTypeValue);
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

  public UserGroup getPatronGroupById(String id) {
    return userReferenceService.getPatronGroupById(id);
  }

  public UserGroup getPatronGroupByName(String name) {
    return userReferenceService.getPatronGroupByName(name);
  }

  public CustomField getCustomFieldByRefId(String refId) {
    return userReferenceService.getCustomFieldByRefId(refId);
  }

  public CustomField getCustomFieldByName(String name)  {
    return userReferenceService.getCustomFieldByName(name);
  }

  public PreferredContactType getPreferredContactTypeById(String id) {
    return userReferenceService.getPreferredContactTypeById(id);
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
