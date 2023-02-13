package org.folio.bulkops.service;

import static org.apache.commons.lang3.ObjectUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.EMPTY;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.bulkops.client.CallNumberTypeClient;
import org.folio.bulkops.client.DamagedStatusClient;
import org.folio.bulkops.client.ItemNoteTypeClient;
import org.folio.bulkops.client.LoanTypeClient;
import org.folio.bulkops.client.LocationClient;
import org.folio.bulkops.client.MaterialTypeClient;
import org.folio.bulkops.client.ServicePointClient;
import org.folio.bulkops.client.StatisticalCodeClient;
import org.folio.bulkops.client.UserClient;
import org.folio.bulkops.domain.bean.ItemLocation;
import org.folio.bulkops.domain.bean.LoanType;
import org.folio.bulkops.domain.bean.MaterialType;
import org.folio.bulkops.exception.NotFoundException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;

@Service
@Log4j2
@RequiredArgsConstructor
public class ItemReferenceService implements InitializingBean {
  private static final String QUERY_PATTERN_NAME = "name==\"%s\"";
  private static final String QUERY_PATTERN_CODE = "code==\"%s\"";
  private static final String QUERY_PATTERN_USERNAME = "username==\"%s\"";

  private final CallNumberTypeClient callNumberTypeClient;
  private final DamagedStatusClient damagedStatusClient;
  private final ItemNoteTypeClient itemNoteTypeClient;
  private final ServicePointClient servicePointClient;
  private final StatisticalCodeClient statisticalCodeClient;
  private final UserClient userClient;
  private final LocationClient locationClient;
  private final MaterialTypeClient materialTypeClient;
  private final LoanTypeClient loanTypeClient;

  public String getCallNumberTypeNameById(String callNumberTypeId) {
    try {
      return isEmpty(callNumberTypeId) ? EMPTY : callNumberTypeClient.getById(callNumberTypeId).getName();
    } catch (NotFoundException e) {
      log.error("Call number type was not found by id={}", callNumberTypeId);
      return callNumberTypeId;
    }
  }

  public String getDamagedStatusNameById(String damagedStatusId) {
    try {
      return isEmpty(damagedStatusId) ? EMPTY : damagedStatusClient.getById(damagedStatusId).getName();
    } catch (NotFoundException e) {
      log.error("Damaged status was not found by id={}", damagedStatusId);
      return damagedStatusId;
    }
  }

  public String getDamagedStatusIdByName(String name) {
    if (isEmpty(name)) {
      return null;
    }
    var response = damagedStatusClient.getByQuery(String.format(QUERY_PATTERN_NAME, name));
    if (response.getItemDamageStatuses().isEmpty()) {
      log.error("Damaged status was not found by name={}", name);
      return name;
    }
    return response.getItemDamageStatuses().get(0).getId();
  }

  public String getNoteTypeNameById(String noteTypeId) {
    try {
      return isEmpty(noteTypeId) ? EMPTY : itemNoteTypeClient.getById(noteTypeId).getName();
    } catch (NotFoundException e) {
      log.error("Note type was not found by id={}", noteTypeId);
      return noteTypeId;
    }
  }

  public String getNoteTypeIdByName(String name) {
    if (isEmpty(name)) {
      return null;
    }
    var response = itemNoteTypeClient.getByQuery(String.format(QUERY_PATTERN_NAME, name));
    if (response.getItemNoteTypes().isEmpty()) {
      log.error("Note type was not found by name={}", name);
      return name;
    }
    return response.getItemNoteTypes().get(0).getId();
  }

  public String getServicePointNameById(String servicePointId) {
    try {
      return isEmpty(servicePointId) ? EMPTY : servicePointClient.getById(servicePointId).getName();
    } catch (NotFoundException e) {
      log.error("Service point was not found by id={}", servicePointId);
      return servicePointId;
    }
  }

  public String getServicePointIdByName(String name) {
    if (isEmpty(name)) {
      return null;
    }
    var response = servicePointClient.get(String.format(QUERY_PATTERN_NAME, name), 1L);
    if (response.getServicepoints().isEmpty()) {
      log.error("Service point was not found by name={}", name);
      return name;
    }
    return response.getServicepoints().get(0).getId();
  }

  public String getStatisticalCodeById(String statisticalCodeId) {
    try {
      return isEmpty(statisticalCodeId) ? EMPTY : statisticalCodeClient.getById(statisticalCodeId).getCode();
    } catch (NotFoundException e) {
      log.error("Statistical code was not found by id={}", statisticalCodeId);
      return statisticalCodeId;
    }
  }

  public String getStatisticalCodeIdByCode(String code) {
    if (isEmpty(code)) {
      return null;
    }
    var response = statisticalCodeClient.getByQuery(String.format(QUERY_PATTERN_CODE, code));
    if (response.getStatisticalCodes().isEmpty()) {
      log.error("Statistical code was not found by code={}", code);
      return code;
    }
    return response.getStatisticalCodes().get(0).getId();
  }

  public String getUserNameById(String userId) {
    try {
      return isEmpty(userId) ? EMPTY : userClient.getUserById(userId).getUsername();
    } catch (NotFoundException e) {
      log.error("User name was not found by id={}", userId);
      return userId;
    }
  }

  public String getUserIdByUserName(String name) {
    if (isEmpty(name)) {
      return null;
    }
    var response = userClient.getUserByQuery(String.format(QUERY_PATTERN_USERNAME, name), 1L);
    if (response.getUsers().isEmpty()) {
      log.error("User was not found by name={}", name);
      return name;
    }
    return response.getUsers().get(0).getId();
  }

  public ItemLocation getLocationByName(String name) {
    var locations = locationClient.getLocationByQuery(String.format(QUERY_PATTERN_NAME, name));
    if (locations.getLocations().isEmpty()) {
      var msg = "Location not found by name=" + name;
      log.error(msg);
      throw new NotFoundException(msg);
    }
    return locations.getLocations().get(0);
  }

  public MaterialType getMaterialTypeByName(String name) {
    var types = materialTypeClient.getByQuery(String.format(QUERY_PATTERN_NAME, name));
    if (types.getMtypes().isEmpty()) {
      log.error("Material type not found by name={}", name);
      throw new NotFoundException("Material type not found: " + name);
    }
    return types.getMtypes().get(0);
  }

  public LoanType getLoanTypeByName(String name) {
    var loanTypes = loanTypeClient.getByQuery(String.format(QUERY_PATTERN_NAME, name));
    if (loanTypes.getLoantypes().isEmpty()) {
      log.error("Loan type not found by name={}", name);
      throw new NotFoundException("Loan type not found: " + name);
    }
    return loanTypes.getLoantypes().get(0);
  }

  private static ItemReferenceService service;

  @Override
  public void afterPropertiesSet() {
    service = this;
  }

  public static ItemReferenceService service() {
    return service;
  }
}