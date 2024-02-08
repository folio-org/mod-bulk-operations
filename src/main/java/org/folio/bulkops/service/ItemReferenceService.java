package org.folio.bulkops.service;

import static java.lang.String.format;
import static org.apache.commons.lang3.ObjectUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.folio.bulkops.util.Constants.BULK_EDIT_CONFIGURATIONS_QUERY_TEMPLATE;
import static org.folio.bulkops.util.Constants.QUERY_PATTERN_CODE;
import static org.folio.bulkops.util.Constants.QUERY_PATTERN_NAME;
import static org.folio.bulkops.util.Constants.QUERY_PATTERN_USERNAME;
import static org.folio.bulkops.util.Utils.encode;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.lang3.ObjectUtils;
import org.folio.bulkops.client.CallNumberTypeClient;
import org.folio.bulkops.client.ConfigurationClient;
import org.folio.bulkops.client.DamagedStatusClient;
import org.folio.bulkops.client.ItemNoteTypeClient;
import org.folio.bulkops.client.LoanTypeClient;
import org.folio.bulkops.client.LocationClient;
import org.folio.bulkops.client.MaterialTypeClient;
import org.folio.bulkops.client.ServicePointClient;
import org.folio.bulkops.client.StatisticalCodeClient;
import org.folio.bulkops.client.UserClient;
import org.folio.bulkops.domain.bean.DamagedStatus;
import org.folio.bulkops.domain.bean.ItemLocation;
import org.folio.bulkops.domain.bean.LoanType;
import org.folio.bulkops.domain.bean.MaterialType;
import org.folio.bulkops.domain.bean.NoteType;
import org.folio.bulkops.domain.bean.ServicePoint;
import org.folio.bulkops.exception.ConfigurationException;
import org.folio.bulkops.exception.NotFoundException;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
@RequiredArgsConstructor
public class ItemReferenceService {

  public static final String MODULE_NAME = "BULKEDIT";
  public static final String STATUSES_CONFIG_NAME = "statuses";

  private final CallNumberTypeClient callNumberTypeClient;
  private final ConfigurationClient configurationClient;
  private final DamagedStatusClient damagedStatusClient;
  private final ItemNoteTypeClient itemNoteTypeClient;
  private final ServicePointClient servicePointClient;
  private final StatisticalCodeClient statisticalCodeClient;
  private final UserClient userClient;
  private final LocationClient locationClient;
  private final MaterialTypeClient materialTypeClient;
  private final LoanTypeClient loanTypeClient;

  private final ObjectMapper objectMapper;

  @Cacheable(cacheNames = "callNumberTypeNames")
  public String getCallNumberTypeNameById(String callNumberTypeId) {
    try {
      return isEmpty(callNumberTypeId) ? EMPTY : callNumberTypeClient.getById(callNumberTypeId).getName();
    } catch (Exception e) {
      throw new NotFoundException(format("Call number type was not found by id=%s", callNumberTypeId));
    }
  }

  @Cacheable(cacheNames = "damagedStatusNames")
  public DamagedStatus getDamagedStatusById(String damagedStatusId) {
    try {
      return damagedStatusClient.getById(damagedStatusId);
    } catch (Exception e) {
      throw new NotFoundException(format("Damaged status was not found by id=%s", damagedStatusId));
    }
  }

  @Cacheable(cacheNames = "damagedStatusIds")
  public DamagedStatus getDamagedStatusByName(String name) {
    var response = damagedStatusClient.getByQuery(String.format(QUERY_PATTERN_NAME, encode(name)));
    if (response.getItemDamageStatuses().isEmpty()) {
      throw new NotFoundException(format("Damaged status was not found by name=%s", name));
    }
    return response.getItemDamageStatuses().get(0);
  }

  @Cacheable(cacheNames = "noteTypeNames")
  public String getNoteTypeNameById(String noteTypeId) {
    try {
      return isEmpty(noteTypeId) ? EMPTY : itemNoteTypeClient.getNoteTypeById(noteTypeId).getName();
    } catch (Exception e) {
      throw new NotFoundException(format("Note type was not found by id=%s", noteTypeId));
    }
  }

  @Cacheable(cacheNames = "noteTypeIds")
  public String getNoteTypeIdByName(String name) {
    var response = itemNoteTypeClient.getNoteTypesByQuery(String.format(QUERY_PATTERN_NAME, encode(name)), 1);
    if (response.getItemNoteTypes().isEmpty()) {
      throw new NotFoundException(format("Note type was not found by name=%s", name));
    }
    return response.getItemNoteTypes().get(0).getId();
  }

  @Cacheable(cacheNames = "servicePointNames")
  public ServicePoint getServicePointById(String servicePointId) {
    try {
      return servicePointClient.getById(servicePointId);
    } catch (Exception e) {
      throw new NotFoundException(format("Service point was not found by id=%s", servicePointId));
    }
  }

  @Cacheable(cacheNames = "servicePointIds")
  public ServicePoint getServicePointByName(String name) {
    var response = servicePointClient.getByQuery(String.format(QUERY_PATTERN_NAME, encode(name)), 1L);
    if (response.getServicepoints().isEmpty()) {
      throw new NotFoundException(format("Service point was not found by name=%s", name));
    }
    return response.getServicepoints().get(0);
  }

  @Cacheable(cacheNames = "statisticalCodeNames")
  public String getStatisticalCodeById(String statisticalCodeId) {
    try {
      return statisticalCodeClient.getById(statisticalCodeId).getCode();
    } catch (Exception e) {
      throw new NotFoundException(format("Statistical code was not found by id=%s", statisticalCodeId));
    }
  }

  @Cacheable(cacheNames = "statisticalCodeIds")
  public String getStatisticalCodeIdByCode(String code) {
    var response = statisticalCodeClient.getByQuery(String.format(QUERY_PATTERN_CODE, encode(code)));
    if (response.getStatisticalCodes().isEmpty()) {
      throw new NotFoundException(format("Statistical code was not found by code=%s", code));
    }
    return response.getStatisticalCodes().get(0).getId();
  }

  @Cacheable(cacheNames = "userNames")
  public String getUserNameById(String userId) {
    try {
      return userClient.getUserById(userId).getUsername();
    } catch (Exception e) {
      throw new NotFoundException(format("User name was not found by id=%s", userId));
    }
  }

  @Cacheable(cacheNames = "userIds")
  public String getUserIdByUserName(String name) {
    var response = userClient.getByQuery(String.format(QUERY_PATTERN_USERNAME, encode(name)), 1L);
    if (response.getUsers().isEmpty()) {
      throw new NotFoundException(format("User was not found by name=%s", name));
    }
    return response.getUsers().get(0).getId();
  }

  @Cacheable(cacheNames = "locations")
  public ItemLocation getLocationById(String id) {
    try {
      return locationClient.getLocationById(id);
    } catch (Exception e) {
      throw new NotFoundException(format("Location was not found by id=%s", id));
    }
  }

  public ItemLocation getLocationByName(String name) {
    var locations = locationClient.getByQuery(String.format(QUERY_PATTERN_NAME, encode(name)));
    if (ObjectUtils.isEmpty(locations) || ObjectUtils.isEmpty(locations.getLocations())) {
      throw new NotFoundException(format("Location not found by name=%s", name));
    }
    return locations.getLocations().get(0);
  }

  public MaterialType getMaterialTypeByName(String name) {
    var types = materialTypeClient.getByQuery(String.format(QUERY_PATTERN_NAME, encode(name)));
    if (types.getMtypes().isEmpty()) {
      throw new NotFoundException(format("Material type not found by name=%s", name));
    }
    return types.getMtypes().get(0);
  }

  @Cacheable(cacheNames = "loanTypes")
  public LoanType getLoanTypeById(String id) {
    try {
      return loanTypeClient.getLoanTypeById(id);
    } catch (Exception e) {
      throw new NotFoundException(format("Loan type not found by id=%s", id));
    }
  }

  public LoanType getLoanTypeByName(String name) {
    var loanTypes = loanTypeClient.getByQuery(String.format(QUERY_PATTERN_NAME, encode(name)));
    if (loanTypes.getLoantypes().isEmpty()) {
      throw new NotFoundException(format("Loan type not found by name=%s", name));
    }
    return loanTypes.getLoantypes().get(0);
  }

  @Cacheable(cacheNames = "statusMapping")
  public List<String> getAllowedStatuses(String statusName) {
    var configurations = configurationClient
      .getByQuery(format(BULK_EDIT_CONFIGURATIONS_QUERY_TEMPLATE, MODULE_NAME, STATUSES_CONFIG_NAME));
    if (configurations.getConfigs()
      .isEmpty()) {
      throw new NotFoundException("Statuses configuration was not found");
    }
    try {
      var statuses = objectMapper.readValue(configurations.getConfigs()
        .get(0)
        .getValue(), new TypeReference<HashMap<String, List<String>>>() {});
      return statuses.getOrDefault(statusName, Collections.emptyList());
    } catch (JsonProcessingException e) {
      throw new ConfigurationException(format("Error reading configuration, reason: %s", e.getMessage()));
    }
  }

  @Cacheable(cacheNames = "itemNoteTypes")
  public List<NoteType> getAllItemNoteTypes() {
    return itemNoteTypeClient.getNoteTypes(Integer.MAX_VALUE).getItemNoteTypes();
  }
}
