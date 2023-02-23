package org.folio.bulkops.service;

import static org.apache.commons.lang3.StringUtils.isEmpty;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.bulkops.domain.bean.CustomField;
import org.folio.bulkops.exception.NotFoundException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Log4j2
public class UserReferenceHelper implements InitializingBean {
  private final UserReferenceService userReferenceService;

  public String getAddressTypeIdByDesc(String desc) {
    if (isEmpty(desc)) {
      return null;
    }
    var res = userReferenceService.getAddressTypeIdByDesc(desc);
    return isEmpty(res) ? desc : res;
  }

  public String getAddressTypeDescById(String id) {
    try {
      return userReferenceService.getAddressTypeDescById(id);
    } catch (NotFoundException e) {
      log.error("Address type was not found by id={}", id);
      return id;
    }
  }

  public String getDepartmentNameById(String id) {
    try {
      return userReferenceService.getDepartmentNameById(id);
    } catch (NotFoundException e) {
      log.error("Department was not found by id={}", id);
      return id;
    }
  }

  public String getDepartmentIdByName(String name) {
    if (isEmpty(name)) {
      return null;
    }
    var res = userReferenceService.getDepartmentIdByName(name);
    return isEmpty(res) ? name : res;
  }

  public String getPatronGroupNameById(String id) {
    try {
      return userReferenceService.getPatronGroupNameById(id);
    } catch (NotFoundException e) {
      log.error("Patron group was not found by id=", id);
      return id;
    }
  }

  public String getPatronGroupIdByName(String name) {
    if (isEmpty(name)) {
      return null;
    }
    var res = userReferenceService.getPatronGroupIdByName(name);
    return isEmpty(res) ? name : res;
  }

  public CustomField getCustomFieldByName(String name)  {
    return userReferenceService.getCustomFieldByName(name);
  }

  public CustomField getCustomFieldByRefId(String refId) {
    return userReferenceService.getCustomFieldByRefId(refId);
  }

  public String getModuleId(String moduleName) {
    return userReferenceService.getModuleId(moduleName);
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
