package org.folio.bulkops.service;

import static java.lang.String.format;
import static org.folio.bulkops.util.Constants.QUERY_PATTERN_DESC;
import static org.folio.bulkops.util.Constants.QUERY_PATTERN_GROUP;
import static org.folio.bulkops.util.Constants.QUERY_PATTERN_NAME;
import static org.folio.bulkops.util.Constants.QUERY_PATTERN_REF_ID;
import static org.folio.bulkops.util.Utils.encode;

import java.net.URI;

import org.apache.commons.lang3.ObjectUtils;
import org.folio.bulkops.client.AddressTypeClient;
import org.folio.bulkops.client.CustomFieldsClient;
import org.folio.bulkops.client.DepartmentClient;
import org.folio.bulkops.client.GroupClient;
import org.folio.bulkops.client.OkapiClient;
import org.folio.bulkops.domain.bean.AddressType;
import org.folio.bulkops.domain.bean.CustomField;
import org.folio.bulkops.domain.bean.Department;
import org.folio.bulkops.domain.bean.UserGroup;
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
  private final CustomFieldsClient customFieldsClient;
  private final FolioExecutionContext folioExecutionContext;
  private final OkapiClient okapiClient;

  @Cacheable(cacheNames = "addressTypeIds")
  public AddressType getAddressTypeByDesc(String desc) {
    var response = addressTypeClient.getByQuery(String.format(QUERY_PATTERN_DESC, encode(desc)));
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
    var response = departmentClient.getByQuery(String.format(QUERY_PATTERN_NAME, encode(name)));
    if (response.getDepartments().isEmpty()) {
      throw new NotFoundException(format("Department=%s not found", name));
    }
    return response.getDepartments().get(0);
  }

  @Cacheable(cacheNames = "patronGroupNames")
  public UserGroup getPatronGroupById(String id) {
    try {
      return groupClient.getGroupById(id);
    } catch (Exception e) {
      throw new NotFoundException(format("Patron group was not found by id=%s", id));
    }
  }

  @Cacheable(cacheNames = "patronGroupIds")
  public UserGroup getPatronGroupByName(String name) {
    var response = groupClient.getByQuery(String.format(QUERY_PATTERN_GROUP, encode(name)));
    if (ObjectUtils.isEmpty(response) || ObjectUtils.isEmpty(response.getUsergroups())) {
      throw new NotFoundException(format("Invalid patron group value: %s", name));
    }
    return response.getUsergroups().get(0);
  }


  @Cacheable(cacheNames = "customFields")
  public CustomField getCustomFieldByName(String name)  {
    return customFieldsClient.getByQuery(getModuleId(MOD_USERS), format(QUERY_PATTERN_NAME, encode(name)))
      .getCustomFields().stream().filter(customField -> customField.getName().equals(name))
      .findFirst()
      .orElseThrow(() -> new NotFoundException(format("Custom field with name=%s not found", name)));
  }

  @Cacheable(cacheNames = "customFields")
  public CustomField getCustomFieldByRefId(String refId) {
    return customFieldsClient.getByQuery(getModuleId(MOD_USERS), format(QUERY_PATTERN_REF_ID, encode(refId)))
      .getCustomFields().stream().filter(customField -> customField.getRefId().equals(refId)).findFirst()
      .orElseThrow(() -> new NotFoundException(format("Custom field with refId=%s not found", refId)));
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
