package org.folio.bulkops.service;

import static java.lang.String.format;

import java.net.URI;

import org.apache.commons.lang3.ObjectUtils;
import org.folio.bulkops.client.AddressTypeClient;
import org.folio.bulkops.client.CustomFieldsClient;
import org.folio.bulkops.client.DepartmentClient;
import org.folio.bulkops.client.GroupClient;
import org.folio.bulkops.client.OkapiClient;
import org.folio.bulkops.client.UserClient;
import org.folio.bulkops.domain.bean.AddressType;
import org.folio.bulkops.domain.bean.CustomField;
import org.folio.bulkops.domain.bean.Department;
import org.folio.bulkops.exception.NotFoundException;
import org.folio.spring.FolioExecutionContext;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@RequiredArgsConstructor
@Log4j2
public class UserReferenceService {
  public static final String OKAPI_URL = "http://_";
  private static final String MOD_USERS = "mod-users";

  private final AddressTypeClient addressTypeClient;
  private final DepartmentClient departmentClient;
  private final GroupClient groupClient;
  private final UserClient userClient;
  private final CustomFieldsClient customFieldsClient;
  private final FolioExecutionContext folioExecutionContext;
  private final OkapiClient okapiClient;

  @Cacheable(cacheNames = "addressTypeIds")
  public AddressType getAddressTypeByDesc(String desc) {
      var response = addressTypeClient.getAddressTypeByQuery(String.format("desc==\"%s\"", desc));
      if (response.getAddressTypes().isEmpty()) {
        throw new NotFoundException(format("Address type=%s not found", desc));
      }
      return response.getAddressTypes().get(0);
  }

  @Cacheable(cacheNames = "addressTypeDesc")
  public AddressType getAddressTypeById(String id) {
    try {
      return addressTypeClient.getAddressTypeById(id);
    } catch (Exception e) {
      throw new NotFoundException(format("Address type was not found by id=%s", id));
    }
  }

  @Cacheable(cacheNames = "departmentNames")
  public Department getDepartmentById(String id) {
    try {
      return departmentClient.getDepartmentById(id);
    } catch (Exception e) {
      throw new NotFoundException(format("Department was not found by id=%s", id));
    }
  }

  @Cacheable(cacheNames = "departmentIds")
  public Department getDepartmentByName(String name) {
      var response = departmentClient.getDepartmentByQuery(String.format("name==\"%s\"", name));
      if (response.getDepartments().isEmpty()) {
        throw new NotFoundException(format("Department=%s not found", name));
      }
      return response.getDepartments().get(0);
  }

  @Cacheable(cacheNames = "patronGroups")
  public String getPatronGroupById(String id) {
    return groupClient.getGroupById(id).getGroup();
  }

  @Cacheable(cacheNames = "patronGroupNames")
  public String getPatronGroupNameById(String id) {
    try {
      return groupClient.getGroupById(id).getGroup();
    } catch (Exception e) {
      throw new NotFoundException(format("Patron group was not found by id=%s", id));
    }
  }

  @Cacheable(cacheNames = "patronGroupIds")
  public String getPatronGroupIdByName(String name) {
    var response = groupClient.getGroupByQuery(String.format("group==\"%s\"", name));
    if (ObjectUtils.isEmpty(response) || ObjectUtils.isEmpty(response.getUsergroups())) {
      throw new NotFoundException(format("Invalid patron group value: %s", name));
    }
    return response.getUsergroups().get(0).getId();
  }


  @Cacheable(cacheNames = "customFields")
  public CustomField getCustomFieldByName(String name)  {
    var customFields = customFieldsClient.getCustomFieldsByQuery(getModuleId(MOD_USERS), String.format("name==\"%s\"", name));
    if (customFields.getCustomFields().isEmpty()) {
      throw new NotFoundException(format("Custom field with name=%s not found", name));
    }
    return customFields.getCustomFields().get(0);
  }

  @Cacheable(cacheNames = "customFields")
  public CustomField getCustomFieldByRefId(String refId) {
    var customFields = customFieldsClient.getCustomFieldsByQuery(getModuleId(MOD_USERS),String.format("refId==\"%s\"", refId));
    if (customFields.getCustomFields().isEmpty()) {
      throw new NotFoundException(format("Custom field with refId=%s not found", refId));
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
    throw new NotFoundException(format("Module id not found for name: %s", moduleName));
  }
}
