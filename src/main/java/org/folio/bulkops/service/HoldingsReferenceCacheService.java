package org.folio.bulkops.service;

import static java.lang.String.format;
import static java.util.Objects.isNull;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.folio.bulkops.util.Constants.QUERY_PATTERN_NAME;
import static org.folio.bulkops.util.Utils.encode;
import static org.folio.spring.utils.FolioExecutionContextUtils.prepareContextForTenant;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.ObjectUtils;
import org.folio.bulkops.client.CallNumberTypeClient;
import org.folio.bulkops.client.HoldingsNoteTypeClient;
import org.folio.bulkops.client.HoldingsSourceClient;
import org.folio.bulkops.client.HoldingsStorageClient;
import org.folio.bulkops.client.HoldingsTypeClient;
import org.folio.bulkops.client.IllPolicyClient;
import org.folio.bulkops.client.LocationClient;
import org.folio.bulkops.client.StatisticalCodeClient;
import org.folio.bulkops.client.StatisticalCodeTypeClient;
import org.folio.bulkops.domain.bean.HoldingsNoteType;
import org.folio.bulkops.domain.bean.HoldingsRecord;
import org.folio.bulkops.domain.bean.HoldingsRecordsSource;
import org.folio.bulkops.domain.bean.HoldingsType;
import org.folio.bulkops.domain.bean.IllPolicy;
import org.folio.bulkops.domain.bean.ItemLocation;
import org.folio.bulkops.domain.bean.StatisticalCode;
import org.folio.bulkops.domain.bean.StatisticalCodeType;
import org.folio.bulkops.exception.NotFoundException;
import org.folio.bulkops.exception.ReferenceDataNotFoundException;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.FolioModuleMetadata;
import org.folio.spring.scope.FolioExecutionContextSetter;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
@Log4j2
public class HoldingsReferenceCacheService {

  private final HoldingsStorageClient holdingsStorageClient;
  private final HoldingsTypeClient holdingsTypeClient;
  private final LocationClient locationClient;
  private final CallNumberTypeClient callNumberTypeClient;
  private final HoldingsNoteTypeClient holdingsNoteTypeClient;
  private final IllPolicyClient illPolicyClient;
  private final HoldingsSourceClient holdingsSourceClient;
  private final StatisticalCodeClient statisticalCodeClient;
  private final FolioModuleMetadata folioModuleMetadata;
  private final StatisticalCodeTypeClient statisticalCodeTypeClient;
  private final FolioExecutionContext folioExecutionContext;
  private final LocalReferenceDataService localReferenceDataService;
  @Lazy private HoldingsReferenceCacheService self;

  @Cacheable(cacheNames = "holdings")
  HoldingsRecord getHoldingsRecordById(String id, String tenantId) {
    return holdingsStorageClient.getHoldingById(id);
  }

  @Cacheable(cacheNames = "holdingsTypesNames")
  HoldingsType getHoldingsTypeById(String id) {
    try (var ignored =
        new FolioExecutionContextSetter(
            prepareContextForTenant(
                localReferenceDataService.getTenantByHoldingsTypeId(id),
                folioModuleMetadata,
                folioExecutionContext))) {
      return holdingsTypeClient.getById(id);
    } catch (Exception e) {
      throw new ReferenceDataNotFoundException(format("Holdings type not found by id=%s", id));
    }
  }

  @Cacheable(cacheNames = "holdingsTypeIds")
  HoldingsType getHoldingsTypeByName(String name, String tenantId) {
    var holdingsTypes = holdingsTypeClient.getByQuery(format(QUERY_PATTERN_NAME, encode(name)));
    if (holdingsTypes.getHoldingsTypes().isEmpty()) {
      throw new ReferenceDataNotFoundException(format("Holdings type not found by name=%s", name));
    }
    return holdingsTypes.getHoldingsTypes().getFirst();
  }

  @Cacheable(cacheNames = "holdingsLocationsNames")
  ItemLocation getLocationById(String id) {
    try (var ignored =
        new FolioExecutionContextSetter(
            prepareContextForTenant(
                localReferenceDataService.getTenantByLocationId(id),
                folioModuleMetadata,
                folioExecutionContext))) {
      return locationClient.getLocationById(id);
    } catch (Exception e) {
      throw new ReferenceDataNotFoundException(format("Location not found by id=%s", id));
    }
  }

  @Cacheable(cacheNames = "holdingsCallNumberTypesNames")
  String getCallNumberTypeNameById(String id) {
    try (var ignored =
        new FolioExecutionContextSetter(
            prepareContextForTenant(
                localReferenceDataService.getTenantByCallNumberTypeId(id),
                folioModuleMetadata,
                folioExecutionContext))) {
      return callNumberTypeClient.getById(id).getName();
    } catch (Exception e) {
      throw new ReferenceDataNotFoundException(format("Call number type not found by id=%s", id));
    }
  }

  @Cacheable(cacheNames = "holdingsCallNumberTypes")
  String getCallNumberTypeIdByName(String name, String tenantId) {
    var callNumberTypes = callNumberTypeClient.getByQuery(format(QUERY_PATTERN_NAME, encode(name)));
    if (callNumberTypes.getCallNumberTypes().isEmpty()) {
      throw new ReferenceDataNotFoundException(
          format("Call number type not found by name=%s", name));
    }
    return callNumberTypes.getCallNumberTypes().getFirst().getId();
  }

  @Cacheable(cacheNames = "holdingsNoteTypesNames")
  String getNoteTypeNameById(String id, String tenantId) {
    if (isNull(tenantId)) {
      tenantId = folioExecutionContext.getTenantId();
    }
    try (var ignored =
        new FolioExecutionContextSetter(
            prepareContextForTenant(tenantId, folioModuleMetadata, folioExecutionContext))) {
      return holdingsNoteTypeClient.getNoteTypeById(id).getName();
    } catch (Exception e) {
      throw new ReferenceDataNotFoundException(format("Note type not found by id=%s", id));
    }
  }

  @Cacheable(cacheNames = "holdingsNoteTypes")
  String getNoteTypeIdByName(String name, String tenantId) {
    var noteTypes =
        holdingsNoteTypeClient.getNoteTypesByQuery(format(QUERY_PATTERN_NAME, encode(name)), 1);
    if (noteTypes.getHoldingsNoteTypes().isEmpty()) {
      throw new ReferenceDataNotFoundException(format("Note type not found by name=%s", name));
    }
    return noteTypes.getHoldingsNoteTypes().getFirst().getId();
  }

  @Cacheable(cacheNames = "illPolicyNames")
  IllPolicy getIllPolicyById(String id) {
    try (var ignored =
        new FolioExecutionContextSetter(
            prepareContextForTenant(
                localReferenceDataService.getTenantByIllPolicyId(id),
                folioModuleMetadata,
                folioExecutionContext))) {
      return illPolicyClient.getById(id);
    } catch (Exception e) {
      throw new ReferenceDataNotFoundException(format("Ill policy not found by id=%s", id));
    }
  }

  @Cacheable(cacheNames = "illPolicies")
  IllPolicy getIllPolicyByName(String name, String tenantId) {
    if (isEmpty(name)) {
      return null;
    }
    var illPolicies = illPolicyClient.getByQuery(format(QUERY_PATTERN_NAME, encode(name)));
    if (illPolicies.getIllPolicies().isEmpty()) {
      throw new ReferenceDataNotFoundException(format("Ill policy not found by name=%s", name));
    }
    return illPolicies.getIllPolicies().getFirst();
  }

  @Cacheable(cacheNames = "holdingsSourceNames")
  HoldingsRecordsSource getSourceById(String id) {
    try (var ignored =
        new FolioExecutionContextSetter(
            prepareContextForTenant(
                localReferenceDataService.getTenantByHoldingsSourceId(id),
                folioModuleMetadata,
                folioExecutionContext))) {
      return isEmpty(id)
          ? HoldingsRecordsSource.builder().name(EMPTY).build()
          : holdingsSourceClient.getById(id);
    } catch (Exception e) {
      log.error(e);
      throw new NotFoundException(format("Holdings record source not found by id=%s", id));
    }
  }

  @Cacheable(cacheNames = "holdingsSources")
  HoldingsRecordsSource getSourceByName(String name, String tenantId) {
    var sources = holdingsSourceClient.getByQuery(format(QUERY_PATTERN_NAME, encode(name)));
    if (ObjectUtils.isEmpty(sources) || ObjectUtils.isEmpty(sources.getHoldingsRecordsSources())) {
      throw new NotFoundException(format("Source not found by name=%s", name));
    }
    return sources.getHoldingsRecordsSources().getFirst();
  }

  @Cacheable(cacheNames = "holdingsStatisticalCodeNames")
  StatisticalCode getStatisticalCodeById(String id, String tenantId) {
    try (var ignored =
        isNull(tenantId)
            ? null
            : new FolioExecutionContextSetter(
                prepareContextForTenant(tenantId, folioModuleMetadata, folioExecutionContext))) {
      return statisticalCodeClient.getById(id);
    } catch (Exception e) {
      throw new ReferenceDataNotFoundException(format("Statistical code not found by id=%s", id));
    }
  }

  @Cacheable(cacheNames = "holdingsStatisticalCodes")
  public StatisticalCode getStatisticalCodeByName(String name, String tenantId) {
    var statisticalCodes =
        statisticalCodeClient.getByQuery(format(QUERY_PATTERN_NAME, encode(name)));
    if (statisticalCodes.getStatisticalCodes().isEmpty()) {
      throw new ReferenceDataNotFoundException(
          format("Statistical code not found by name=%s", name));
    }
    return statisticalCodes.getStatisticalCodes().getFirst();
  }

  @Cacheable(cacheNames = "holdingsStatisticalCodeTypes")
  public StatisticalCodeType getStatisticalCodeTypeById(String id, String tenantId) {
    try (var ignored =
        isNull(tenantId)
            ? null
            : new FolioExecutionContextSetter(
                prepareContextForTenant(tenantId, folioModuleMetadata, folioExecutionContext))) {
      return statisticalCodeTypeClient.getById(id);
    } catch (Exception e) {
      throw new ReferenceDataNotFoundException(
          format("Statistical code type not found by id=%s", id));
    }
  }

  @Cacheable(cacheNames = "holdingsNoteTypes")
  public List<HoldingsNoteType> getAllHoldingsNoteTypes(String tenantId) {
    var noteTypes = holdingsNoteTypeClient.getNoteTypes(Integer.MAX_VALUE).getHoldingsNoteTypes();
    noteTypes.forEach(nt -> nt.setTenantId(tenantId));
    return noteTypes;
  }

  @Cacheable(cacheNames = "holdingsJsons")
  public JsonNode getHoldingsJsonById(String holdingsId, String tenantId) {
    try (var context =
        new FolioExecutionContextSetter(
            prepareContextForTenant(tenantId, folioModuleMetadata, folioExecutionContext))) {
      return holdingsStorageClient.getHoldingsJsonById(holdingsId);
    }
  }

  @Cacheable(cacheNames = "holdingsLocations")
  public JsonNode getHoldingsLocationById(String locationId, String tenantId) {
    if (ObjectUtils.isEmpty(locationId)) {
      return new ObjectMapper().createObjectNode();
    }
    try (var context =
        new FolioExecutionContextSetter(
            prepareContextForTenant(tenantId, folioModuleMetadata, folioExecutionContext))) {
      return locationClient.getLocationJsonById(locationId);
    }
  }
}
