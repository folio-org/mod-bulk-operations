package org.folio.bulkops.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.ObjectUtils;
import org.folio.bulkops.client.AddressTypeClient;
import org.folio.bulkops.client.CustomFieldsClient;
import org.folio.bulkops.client.DepartmentClient;
import org.folio.bulkops.client.GroupClient;
import org.folio.bulkops.client.OkapiClient;
import org.folio.bulkops.domain.bean.CustomField;
import org.folio.spring.FolioExecutionContext;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.net.URI;

import static java.lang.String.format;
import static java.util.Objects.isNull;
import static org.apache.commons.lang3.ObjectUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.EMPTY;

@Service
@RequiredArgsConstructor
@Log4j2
public class UserReferenceService implements InitializingBean {
  private static final String OKAPI_URL = "http://_";
  private static final String MOD_USERS = "mod-users";

  private final AddressTypeClient addressTypeClient;
  private final DepartmentClient departmentClient;
  private final GroupClient groupClient;
  private final CustomFieldsClient customFieldsClient;
  private final FolioExecutionContext folioExecutionContext;
  private final OkapiClient okapiClient;

  @Cacheable(cacheNames = "addressTypeIds")
  public String getAddressTypeIdByDesc(String desc) {
    if (isEmpty(desc)) {
      return null;
    } else {
      var response = addressTypeClient.getAddressTypeByQuery(String.format("desc==\"%s\"", desc));
      if (response.getAddressTypes().isEmpty()) {
        var msg = format("Address type=%s not found", desc);
        log.error(msg);
        return EMPTY;
      }
      return response.getAddressTypes().get(0).getId();
    }
  }

  public String getAddressTypeDescById(String id) {
      return isNull(id) ? EMPTY : addressTypeClient.getAddressTypeById(id).getDesc();
  }

  public String getDepartmentNameById(String id) {
    return isNull(id) ? EMPTY : departmentClient.getDepartmentById(id).getName();
  }


  @Cacheable(cacheNames = "departmentIds")
  public String getDepartmentIdByName(String name) {
    if (isEmpty(name)) {
      return EMPTY;
    } else {
      var response = departmentClient.getDepartmentByQuery(String.format("name==\"%s\"", name));
      if (response.getDepartments().isEmpty()) {
        var msg = format("Department=%s not found", name);
        log.error(msg);
        return EMPTY;
      }
      return response.getDepartments().get(0).getId();
    }
  }

  public String getPatronGroupById(String id) {
    return groupClient.getGroupById(id).getGroup();
  }

  public String getPatronGroupNameById(String id) {
      return isNull(id) ? EMPTY : groupClient.getGroupById(id).getGroup();
  }

  @Cacheable(cacheNames = "patronGroupIds")
  public String getPatronGroupIdByName(String name) {
    if (isEmpty(name)) {
      return EMPTY;
    }
    var response = groupClient.getGroupByQuery(String.format("group==\"%s\"", name));
    if (ObjectUtils.isEmpty(response) || ObjectUtils.isEmpty(response.getUsergroups())) {
      var msg = "Invalid patron group value: " + name;
      log.error(msg);
      return EMPTY;
    }
    return response.getUsergroups().get(0).getId();
  }


  @Cacheable(cacheNames = "customFields")
  public CustomField getCustomFieldByName(String name)  {
    var customFields = customFieldsClient.getCustomFieldsByQuery(getModuleId(MOD_USERS), String.format("name==\"%s\"", name));
    if (customFields.getCustomFields().isEmpty()) {
      var msg = format("Custom field with name=%s not found", name);
      log.error(msg);
      return new CustomField();
    }
    return customFields.getCustomFields().get(0);
  }

  @Cacheable(cacheNames = "customFields")
  public CustomField getCustomFieldByRefId(String refId) {
    var customFields = customFieldsClient.getCustomFieldsByQuery(getModuleId(MOD_USERS),String.format("refId==\"%s\"", refId));
    if (customFields.getCustomFields().isEmpty()) {
      var msg = format("Custom field with refId=%s not found", refId);
      log.error(msg);
      return new CustomField();
    }
    return customFields.getCustomFields().get(0);
  }

  @Cacheable(cacheNames = "moduleIds")
  public String getModuleId(String moduleName) {
    var tenantId = folioExecutionContext.getTenantId();
    var moduleNamesJson = okapiClient.getModuleIds(URI.create(OKAPI_URL), tenantId, moduleName);
    if (!moduleNamesJson.isEmpty()) {
      return moduleNamesJson.get(0).get("id").asText();
    }
    var msg = "Module id not found for name: " + moduleName;
    log.error(msg);
    return EMPTY;
  }

  private static UserReferenceService service = null;

  @Override
  public void afterPropertiesSet() {
    service = this;
  }

  public static UserReferenceService service() {
    return service;
  }
}
