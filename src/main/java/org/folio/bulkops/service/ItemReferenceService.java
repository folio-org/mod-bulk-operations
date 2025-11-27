package org.folio.bulkops.service;

import static java.lang.String.format;
import static java.util.Objects.isNull;
import static org.apache.commons.lang3.ObjectUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.folio.bulkops.util.Constants.QUERY_PATTERN_CODE;
import static org.folio.bulkops.util.Constants.QUERY_PATTERN_NAME;
import static org.folio.bulkops.util.Constants.QUERY_PATTERN_USERNAME;
import static org.folio.bulkops.util.FolioExecutionContextUtil.prepareContextForTenant;
import static org.folio.bulkops.util.Utils.encode;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.ObjectUtils;
import org.folio.bulkops.client.CallNumberTypeClient;
import org.folio.bulkops.client.DamagedStatusClient;
import org.folio.bulkops.client.ItemNoteTypeClient;
import org.folio.bulkops.client.LoanTypeClient;
import org.folio.bulkops.client.LocationClient;
import org.folio.bulkops.client.MaterialTypeClient;
import org.folio.bulkops.client.ServicePointClient;
import org.folio.bulkops.client.StatisticalCodeClient;
import org.folio.bulkops.client.StatisticalCodeTypeClient;
import org.folio.bulkops.client.UserClient;
import org.folio.bulkops.domain.bean.DamagedStatus;
import org.folio.bulkops.domain.bean.ItemLocation;
import org.folio.bulkops.domain.bean.LoanType;
import org.folio.bulkops.domain.bean.MaterialType;
import org.folio.bulkops.domain.bean.NoteType;
import org.folio.bulkops.domain.bean.ServicePoint;
import org.folio.bulkops.domain.bean.StatisticalCode;
import org.folio.bulkops.domain.bean.StatisticalCodeType;
import org.folio.bulkops.domain.entity.AllowedItemStatuses;
import org.folio.bulkops.exception.ReferenceDataNotFoundException;
import org.folio.bulkops.repository.AllowedItemStatusesRepository;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.FolioModuleMetadata;
import org.folio.spring.scope.FolioExecutionContextSetter;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@Log4j2
@RequiredArgsConstructor
public class ItemReferenceService {

  private final CallNumberTypeClient callNumberTypeClient;
  private final DamagedStatusClient damagedStatusClient;
  private final ItemNoteTypeClient itemNoteTypeClient;
  private final ServicePointClient servicePointClient;
  private final StatisticalCodeClient statisticalCodeClient;
  private final UserClient userClient;
  private final LocationClient locationClient;
  private final MaterialTypeClient materialTypeClient;
  private final LoanTypeClient loanTypeClient;
  private final FolioExecutionContext folioExecutionContext;
  private final FolioModuleMetadata folioModuleMetadata;
  private final StatisticalCodeTypeClient statisticalCodeTypeClient;
  private final LocalReferenceDataService localReferenceDataService;
  private final AllowedItemStatusesRepository allowedItemStatusesRepository;

  private final ObjectMapper objectMapper;

  @Cacheable(cacheNames = "callNumberTypeNames")
  public String getCallNumberTypeNameById(String callNumberTypeId) {
    try {
      return isEmpty(callNumberTypeId) ? EMPTY
              : callNumberTypeClient.getById(callNumberTypeId).getName();
    } catch (Exception e) {
      throw new ReferenceDataNotFoundException(format("Call number type was not found by id=%s",
              callNumberTypeId));
    }
  }

  @Cacheable(cacheNames = "damagedStatusNames")
  public DamagedStatus getDamagedStatusById(String damagedStatusId) {
    try {
      return damagedStatusClient.getById(damagedStatusId);
    } catch (Exception e) {
      throw new ReferenceDataNotFoundException(format("Damaged status was not found by id=%s",
              damagedStatusId));
    }
  }

  @Cacheable(cacheNames = "damagedStatusIds")
  public DamagedStatus getDamagedStatusByName(String name) {
    var response = damagedStatusClient.getByQuery(String.format(QUERY_PATTERN_NAME, encode(name)));
    if (response.getItemDamageStatuses().isEmpty()) {
      throw new ReferenceDataNotFoundException(format("Damaged status was not found by name=%s",
              name));
    }
    return response.getItemDamageStatuses().getFirst();
  }

  @Cacheable(cacheNames = "noteTypeNames")
  public String getNoteTypeNameById(String noteTypeId, String tenantId) {
    if (isNull(tenantId)) {
      tenantId = folioExecutionContext.getTenantId();
    }
    try (var ignored = new FolioExecutionContextSetter(prepareContextForTenant(tenantId,
            folioModuleMetadata, folioExecutionContext))) {
      return isEmpty(noteTypeId) ? EMPTY : itemNoteTypeClient.getNoteTypeById(noteTypeId).getName();
    } catch (Exception e) {
      throw new ReferenceDataNotFoundException(format("Note type was not found by id=%s",
              noteTypeId));
    }
  }

  @Cacheable(cacheNames = "noteTypeIds")
  public String getNoteTypeIdByName(String name) {
    var response = itemNoteTypeClient.getNoteTypesByQuery(
            String.format(QUERY_PATTERN_NAME, encode(name)), 1);
    if (response.getItemNoteTypes().isEmpty()) {
      throw new ReferenceDataNotFoundException(format("Note type was not found by name=%s", name));
    }
    return response.getItemNoteTypes().getFirst().getId();
  }

  @Cacheable(cacheNames = "servicePointNames")
  public ServicePoint getServicePointById(String servicePointId) {
    try {
      return servicePointClient.getById(servicePointId);
    } catch (Exception e) {
      throw new ReferenceDataNotFoundException(format("Service point was not found by id=%s",
              servicePointId));
    }
  }

  @Cacheable(cacheNames = "servicePointIds")
  public ServicePoint getServicePointByName(String name) {
    var response = servicePointClient.getByQuery(String.format(QUERY_PATTERN_NAME, encode(name)),
            1L);
    if (response.getServicepoints().isEmpty()) {
      throw new ReferenceDataNotFoundException(format("Service point was not found by name=%s",
              name));
    }
    return response.getServicepoints().getFirst();
  }

  @Cacheable(cacheNames = "statisticalCodeNames")
  public StatisticalCode getStatisticalCodeById(String statisticalCodeId, String tenantId) {
    try (var ignored = isNull(tenantId)
        ? null
        : new FolioExecutionContextSetter(prepareContextForTenant(tenantId,
            folioModuleMetadata, folioExecutionContext))) {
      return statisticalCodeClient.getById(statisticalCodeId);
    } catch (Exception e) {
      throw new ReferenceDataNotFoundException(format("Statistical code was not found by id=%s",
              statisticalCodeId));
    }
  }

  @Cacheable(cacheNames = "statisticalCodeIds")
  public String getStatisticalCodeIdByCode(String code) {
    var response = statisticalCodeClient.getByQuery(String.format(QUERY_PATTERN_CODE,
            encode(code)));
    if (response.getStatisticalCodes().isEmpty()) {
      throw new ReferenceDataNotFoundException(format("Statistical code was not found by code=%s",
              code));
    }
    return response.getStatisticalCodes().getFirst().getId();
  }

  @Cacheable(cacheNames = "statisticalCodeTypes")
  public StatisticalCodeType getStatisticalCodeTypeById(String id, String tenantId) {
    try (var ignored = isNull(tenantId)
        ? null
        : new FolioExecutionContextSetter(prepareContextForTenant(tenantId,
            folioModuleMetadata, folioExecutionContext))) {
      return statisticalCodeTypeClient.getById(id);
    } catch (Exception e) {
      throw new ReferenceDataNotFoundException(format("Statistical code type not found by id=%s",
              id));
    }
  }

  @Cacheable(cacheNames = "userNames")
  public String getUserNameById(String userId) {
    try {
      return userClient.getUserById(userId).getUsername();
    } catch (Exception e) {
      throw new ReferenceDataNotFoundException(format("User name was not found by id=%s", userId));
    }
  }

  @Cacheable(cacheNames = "userIds")
  public String getUserIdByUserName(String name) {
    var response = userClient.getByQuery(String.format(QUERY_PATTERN_USERNAME, encode(name)), 1L);
    if (response.getUsers().isEmpty()) {
      throw new ReferenceDataNotFoundException(format("User was not found by name=%s", name));
    }
    return response.getUsers().getFirst().getId();
  }

  @Cacheable(cacheNames = "locations")
  public ItemLocation getLocationById(String id, String tenantId) {
    if (isNull(tenantId)) {
      tenantId = folioExecutionContext.getTenantId();
    }
    try (var ignored = new FolioExecutionContextSetter(prepareContextForTenant(tenantId,
            folioModuleMetadata, folioExecutionContext))) {
      return locationClient.getLocationById(id);
    } catch (Exception e) {
      throw new ReferenceDataNotFoundException(format("Location was not found by id=%s", id));
    }
  }

  public ItemLocation getLocationById(String id) {
    try {
      return locationClient.getLocationById(id);
    } catch (Exception e) {
      throw new ReferenceDataNotFoundException(format("Location not found by id=%s", id));
    }
  }

  public ItemLocation getLocationByName(String name) {
    var locations = locationClient.getByQuery(String.format(QUERY_PATTERN_NAME, encode(name)));
    if (ObjectUtils.isEmpty(locations) || ObjectUtils.isEmpty(locations.getLocations())) {
      throw new ReferenceDataNotFoundException(format("Location not found by name=%s", name));
    }
    return locations.getLocations().getFirst();
  }

  public MaterialType getMaterialTypeByName(String name) {
    var types = materialTypeClient.getByQuery(String.format(QUERY_PATTERN_NAME, encode(name)));
    if (types.getMtypes().isEmpty()) {
      throw new ReferenceDataNotFoundException(format("Material type not found by name=%s", name));
    }
    return types.getMtypes().getFirst();
  }

  @Cacheable(cacheNames = "materialTypes")
  public MaterialType getMaterialTypeById(String id, String tenantId) {
    if (isNull(tenantId)) {
      tenantId = folioExecutionContext.getTenantId();
    }
    try (var ignored = new FolioExecutionContextSetter(prepareContextForTenant(tenantId,
            folioModuleMetadata, folioExecutionContext))) {
      return materialTypeClient.getById(id);
    } catch (Exception e) {
      throw new ReferenceDataNotFoundException(format("Material type not found by id=%s", id));
    }
  }

  @Cacheable(cacheNames = "loanTypes")
  public LoanType getLoanTypeById(String id, String tenantId) {
    if (isNull(tenantId)) {
      tenantId = folioExecutionContext.getTenantId();
    }
    try (var ignored = new FolioExecutionContextSetter(prepareContextForTenant(tenantId,
            folioModuleMetadata, folioExecutionContext))) {
      return loanTypeClient.getLoanTypeById(id);
    } catch (Exception e) {
      throw new ReferenceDataNotFoundException(format("Loan type not found by id=%s", id));
    }
  }

  public LoanType getLoanTypeByName(String name) {
    var loanTypes = loanTypeClient.getByQuery(String.format(QUERY_PATTERN_NAME, encode(name)));
    if (loanTypes.getLoantypes().isEmpty()) {
      throw new ReferenceDataNotFoundException(format("Loan type not found by name=%s", name));
    }
    return loanTypes.getLoantypes().getFirst();
  }

  @Cacheable(cacheNames = "statusMapping")
  public List<String> getAllowedStatuses(String statusName) {
    return allowedItemStatusesRepository.findByStatus(statusName)
      .map(AllowedItemStatuses::getAllowedStatuses)
      .orElse(Collections.emptyList());
  }

  @Cacheable(cacheNames = "itemNoteTypes")
  public List<NoteType> getAllItemNoteTypes(String tenantId) {
    var noteTypes = itemNoteTypeClient.getNoteTypes(Integer.MAX_VALUE).getItemNoteTypes();
    noteTypes.forEach(noteType -> noteType.setTenantId(tenantId));
    return noteTypes;
  }
}
