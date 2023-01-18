package org.folio.bulkops.adapters.impl.users;

import static java.lang.String.format;
import static java.util.Objects.isNull;
import static org.apache.commons.lang3.StringUtils.EMPTY;

import java.net.URI;
import java.util.Objects;
import java.util.UUID;

import org.folio.bulkops.client.AddressTypeClient;
import org.folio.bulkops.client.CustomFieldsClient;
import org.folio.bulkops.client.DepartmentClient;
import org.folio.bulkops.client.GroupClient;
import org.folio.bulkops.client.OkapiClient;
import org.folio.bulkops.domain.bean.CustomField;
import org.folio.bulkops.exception.NotFoundException;
import org.folio.bulkops.service.ErrorService;
import org.folio.spring.FolioExecutionContext;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Component
@Log4j2
@RequiredArgsConstructor
public class UserReferenceResolver {
  private static final String OKAPI_URL = "http://_";
  private static final String MOD_USERS = "mod-users";
  private final AddressTypeClient addressTypeClient;
  private final DepartmentClient departmentClient;
  private final GroupClient groupClient;
  private final CustomFieldsClient customFieldsClient;
  private final FolioExecutionContext folioExecutionContext;
  private final OkapiClient okapiClient;
  private final ErrorService errorService;

  @Cacheable(cacheNames = "addressTypeNames")
  public String getAddressTypeDescById(String id, UUID bulkOperationId, String identifier) {
    try {
      return isNull(id) ? EMPTY
          : addressTypeClient.getAddressTypeById(id)
            .getDesc();
    } catch (NotFoundException e) {
      var msg = String.format("Address type was not found by id: [%s]", id);
      log.error(msg);
      if (Objects.nonNull(bulkOperationId)) {
        errorService.saveError(bulkOperationId, identifier, msg);
      }
      return id;
    }
  }

  @Cacheable(cacheNames = "departmentNames")
  public String getDepartmentNameById(String id, UUID bulkOperationId, String identifier) {
    try {
      return isNull(id) ? EMPTY
          : departmentClient.getDepartmentById(id)
            .getName();
    } catch (NotFoundException e) {
      var msg = String.format("Department was not found by id: [%s]", id);
      log.error(msg);
      if (Objects.nonNull(bulkOperationId)) {
        errorService.saveError(bulkOperationId, identifier, msg);
      }
      return id;
    }
  }

  @Cacheable(cacheNames = "patronGroupNames")
  public String getPatronGroupNameById(String id, UUID bulkOperationId, String identifier) {
    try {
      return isNull(id) ? EMPTY
          : groupClient.getGroupById(id)
            .getGroup();
    } catch (NotFoundException e) {
      var msg = String.format("Patron group was not found by id: [%s]", id);
      log.error(msg);
      if (Objects.nonNull(bulkOperationId)) {
        errorService.saveError(bulkOperationId, identifier, msg);
      }
      return id;
    }
  }

  @Cacheable(cacheNames = "customFields")
  public CustomField getCustomFieldByRefId(String refId) {
    var customFields = customFieldsClient.getCustomFieldsByQuery(getModuleId(MOD_USERS), String.format("refId==\"%s\"", refId));
    if (customFields.getCustomFields().isEmpty()) {
      var msg = format("Custom field with refId=%s not found", refId);
      log.error(msg);
      return new CustomField().withName(refId);
    }
    return customFields.getCustomFields().get(0);
  }

  @Cacheable(cacheNames = "moduleIds")
  public String getModuleId(String moduleName) {
    var tenantId = folioExecutionContext.getTenantId();
    var moduleNamesJson = okapiClient.getModuleIds(URI.create(OKAPI_URL), tenantId, moduleName);
    if (!moduleNamesJson.isEmpty()) {
      return moduleNamesJson.get(0)
        .get("id")
        .asText();
    }
    var msg = "Module id not found for name: " + moduleName;
    log.error(msg);
    throw new NotFoundException(msg);
  }
}
