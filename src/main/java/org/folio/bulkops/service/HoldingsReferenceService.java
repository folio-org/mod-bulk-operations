package org.folio.bulkops.service;

import static java.lang.String.format;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.SPACE;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.folio.bulkops.util.Constants.CALL_NUMBER;
import static org.folio.bulkops.util.Constants.CALL_NUMBER_PREFIX;
import static org.folio.bulkops.util.Constants.CALL_NUMBER_SUFFIX;
import static org.folio.bulkops.util.Constants.HOLDINGS_LOCATION_CALL_NUMBER_DELIMITER;
import static org.folio.bulkops.util.Constants.INACTIVE;
import static org.folio.bulkops.util.Constants.IS_ACTIVE;
import static org.folio.bulkops.util.Constants.NAME;
import static org.folio.bulkops.util.Constants.PERMANENT_LOCATION_ID;
import static org.folio.bulkops.util.Constants.QUERY_PATTERN_BARCODE;
import static org.folio.bulkops.util.Constants.QUERY_PATTERN_HRID;
import static org.folio.bulkops.util.Constants.QUERY_PATTERN_NAME;
import static org.folio.bulkops.util.FolioExecutionContextUtil.prepareContextForTenant;
import static org.folio.bulkops.util.Utils.encode;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.ObjectUtils;
import org.folio.bulkops.client.CallNumberTypeClient;
import org.folio.bulkops.client.HoldingsStorageClient;
import org.folio.bulkops.client.HoldingsNoteTypeClient;
import org.folio.bulkops.client.HoldingsSourceClient;
import org.folio.bulkops.client.HoldingsTypeClient;
import org.folio.bulkops.client.IllPolicyClient;
import org.folio.bulkops.client.InstanceClient;
import org.folio.bulkops.client.InstanceStorageClient;
import org.folio.bulkops.client.ItemClient;
import org.folio.bulkops.client.LocationClient;
import org.folio.bulkops.client.StatisticalCodeClient;
import org.folio.bulkops.domain.bean.HoldingsNoteType;
import org.folio.bulkops.domain.bean.HoldingsRecord;
import org.folio.bulkops.domain.bean.HoldingsRecordsSource;
import org.folio.bulkops.domain.bean.HoldingsType;
import org.folio.bulkops.domain.bean.IllPolicy;
import org.folio.bulkops.domain.bean.ItemLocation;
import org.folio.bulkops.domain.bean.StatisticalCode;
import org.folio.bulkops.domain.dto.ErrorType;
import org.folio.bulkops.exception.BulkEditException;
import org.folio.bulkops.exception.NotFoundException;
import org.folio.bulkops.exception.ReferenceDataNotFoundException;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.FolioModuleMetadata;
import org.folio.spring.scope.FolioExecutionContextSetter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Log4j2
public class HoldingsReferenceService {

  private final HoldingsStorageClient holdingsStorageClient;
  private final HoldingsTypeClient holdingsTypeClient;
  private final LocationClient locationClient;
  private final CallNumberTypeClient callNumberTypeClient;
  private final HoldingsNoteTypeClient holdingsNoteTypeClient;
  private final IllPolicyClient illPolicyClient;
  private final HoldingsSourceClient holdingsSourceClient;
  private final StatisticalCodeClient statisticalCodeClient;
  private final InstanceClient instanceClient;
  private final InstanceStorageClient instanceStorageClient;
  private final ItemClient itemClient;
  private final FolioModuleMetadata folioModuleMetadata;
  private final FolioExecutionContext folioExecutionContext;
  private final LocalReferenceDataService localReferenceDataService;
  @Lazy private HoldingsReferenceService self;

  @Cacheable(cacheNames = "holdings")
  public HoldingsRecord getHoldingsRecordById(String id, String tenantId) {
    return holdingsStorageClient.getHoldingById(id);
  }

  @Cacheable(cacheNames = "holdingsTypesNames")
  public HoldingsType getHoldingsTypeById(String id) {
    try (var ignored = new FolioExecutionContextSetter(prepareContextForTenant(localReferenceDataService.getTenantByHoldingsTypeId(id), folioModuleMetadata, folioExecutionContext))) {
      return holdingsTypeClient.getById(id);
    } catch (Exception e) {
      throw new ReferenceDataNotFoundException(format("Holdings type not found by id=%s", id));
    }
  }

  @Cacheable(cacheNames = "holdingsTypeIds")
  public HoldingsType getHoldingsTypeByName(String name, String tenantId) {
    var holdingsTypes = holdingsTypeClient.getByQuery(format(QUERY_PATTERN_NAME, encode(name)));
    if (holdingsTypes.getHoldingsTypes().isEmpty()) {
      throw new ReferenceDataNotFoundException(format("Holdings type not found by name=%s", name));
    }
    return holdingsTypes.getHoldingsTypes().getFirst();
  }

  @Cacheable(cacheNames = "holdingsLocationsNames")
  public ItemLocation getLocationById(String id) {
    try (var ignored = new FolioExecutionContextSetter(prepareContextForTenant(localReferenceDataService.getTenantByLocationId(id), folioModuleMetadata, folioExecutionContext))) {
      return locationClient.getLocationById(id);
    } catch (Exception e) {
      throw new ReferenceDataNotFoundException(format("Location not found by id=%s", id));
    }
  }

  public ItemLocation getLocationIdByName(String name) {
    var locations = locationClient.getByQuery(format(QUERY_PATTERN_NAME, encode(name)));
    if (locations.getLocations().isEmpty()) {
      throw new ReferenceDataNotFoundException(format("Location not found by name=%s", name));
    }
    return locations.getLocations().getFirst();
  }

  @Cacheable(cacheNames = "holdingsCallNumberTypesNames")
  public String getCallNumberTypeNameById(String id) {
    try (var ignored = new FolioExecutionContextSetter(prepareContextForTenant(localReferenceDataService.getTenantByCallNumberTypeId(id), folioModuleMetadata, folioExecutionContext))) {
      return  callNumberTypeClient.getById(id).getName();
    } catch (Exception e) {
      throw new ReferenceDataNotFoundException(format("Call number type not found by id=%s", id));
    }
  }

  @Cacheable(cacheNames = "holdingsCallNumberTypes")
  public String getCallNumberTypeIdByName(String name, String tenantId) {
    var callNumberTypes = callNumberTypeClient.getByQuery(format(QUERY_PATTERN_NAME, encode(name)));
    if (callNumberTypes.getCallNumberTypes().isEmpty()) {
      throw new ReferenceDataNotFoundException(format("Call number type not found by name=%s", name));
    }
    return callNumberTypes.getCallNumberTypes().getFirst().getId();
  }

  @Cacheable(cacheNames = "holdingsNoteTypesNames")
  public String getNoteTypeNameById(String id, String tenantId) {
    if (isNull(tenantId)) {
      tenantId = folioExecutionContext.getTenantId();
    }
    try (var ignored = new FolioExecutionContextSetter(prepareContextForTenant(tenantId, folioModuleMetadata, folioExecutionContext))) {
      return holdingsNoteTypeClient.getNoteTypeById(id).getName();
    } catch (Exception e) {
      throw new ReferenceDataNotFoundException(format("Note type not found by id=%s", id));
    }
  }

  @Cacheable(cacheNames = "holdingsNoteTypes")
  public String getNoteTypeIdByName(String name, String tenantId) {
    var noteTypes = holdingsNoteTypeClient.getNoteTypesByQuery(format(QUERY_PATTERN_NAME, encode(name)), 1);
    if (noteTypes.getHoldingsNoteTypes().isEmpty()) {
      throw new ReferenceDataNotFoundException(format("Note type not found by name=%s", name));
    }
    return noteTypes.getHoldingsNoteTypes().getFirst().getId();
  }

  @Cacheable(cacheNames = "illPolicyNames")
  public IllPolicy getIllPolicyById(String id) {
    try (var ignored = new FolioExecutionContextSetter(prepareContextForTenant(localReferenceDataService.getTenantByIllPolicyId(id), folioModuleMetadata, folioExecutionContext))) {
      return illPolicyClient.getById(id);
    } catch (Exception e) {
      throw new ReferenceDataNotFoundException(format("Ill policy not found by id=%s", id));
    }
  }

  @Cacheable(cacheNames = "illPolicies")
  public IllPolicy getIllPolicyByName(String name, String tenantId) {
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
  public HoldingsRecordsSource getSourceById(String id) {
    try (var ignored = new FolioExecutionContextSetter(prepareContextForTenant(localReferenceDataService.getTenantByHoldingsSourceId(id), folioModuleMetadata, folioExecutionContext))) {
      return isEmpty(id) ?
        HoldingsRecordsSource.builder().name(EMPTY).build() :
        holdingsSourceClient.getById(id);
    } catch (Exception e) {
      log.error(e);
      throw new NotFoundException(format("Holdings record source not found by id=%s", id));
    }
  }

  @Cacheable(cacheNames = "holdingsSources")
  public HoldingsRecordsSource getSourceByName(String name, String tenantId) {
    var sources = holdingsSourceClient.getByQuery(format(QUERY_PATTERN_NAME, encode(name)));
    if (ObjectUtils.isEmpty(sources) || ObjectUtils.isEmpty(sources.getHoldingsRecordsSources())) {
      throw new NotFoundException(format("Source not found by name=%s", name));
    }
    return sources.getHoldingsRecordsSources().getFirst();
  }

  @Cacheable(cacheNames = "holdingsStatisticalCodeNames")
  public StatisticalCode getStatisticalCodeById(String id) {
    try (var ignored = new FolioExecutionContextSetter(prepareContextForTenant(localReferenceDataService.getTenantByStatisticalCodeId(id), folioModuleMetadata, folioExecutionContext))) {
      return statisticalCodeClient.getById(id);
    } catch (Exception e) {
      throw new ReferenceDataNotFoundException(format("Statistical code not found by id=%s", id));
    }
  }

  @Cacheable(cacheNames = "holdingsStatisticalCodes")
  public StatisticalCode getStatisticalCodeByName(String name, String tenantId) {
    var statisticalCodes = statisticalCodeClient.getByQuery(format(QUERY_PATTERN_NAME, encode(name)));
    if (statisticalCodes.getStatisticalCodes().isEmpty()) {
      throw new ReferenceDataNotFoundException(format("Statistical code not found by name=%s", name));
    }
    return statisticalCodes.getStatisticalCodes().getFirst();
  }

  @Cacheable(cacheNames = "holdingsNoteTypes")
  public List<HoldingsNoteType> getAllHoldingsNoteTypes(String tenantId) {
    var noteTypes = holdingsNoteTypeClient.getNoteTypes(Integer.MAX_VALUE).getHoldingsNoteTypes();
    noteTypes.forEach(nt -> nt.setTenantId(tenantId));
    return noteTypes;
  }

  public String getInstanceIdByHrid(String instanceHrid) {
    var briefInstances = instanceClient.getByQuery(String.format(QUERY_PATTERN_HRID, instanceHrid));
    if (briefInstances.getInstances().isEmpty()) {
      throw new BulkEditException("Instance not found by hrid=" + instanceHrid, ErrorType.WARNING);
    } else {
      return briefInstances.getInstances().getFirst().getId();
    }
  }

  public String getHoldingsIdByItemBarcode(String itemBarcode) {
    var items = itemClient.getByQuery(String.format(QUERY_PATTERN_BARCODE, itemBarcode), 1);
    if (items.getItems().isEmpty()) {
      throw new BulkEditException("Item not found by barcode=" + itemBarcode, ErrorType.WARNING);
    }
    return items.getItems().getFirst().getHoldingsRecordId();
  }

  public String getInstanceTitleById(String instanceId, String tenantId) {
    if (isEmpty(instanceId)) {
      return EMPTY;
    }
    try (var context = new FolioExecutionContextSetter(prepareContextForTenant(tenantId, folioModuleMetadata, folioExecutionContext))) {
      var instanceJson = instanceStorageClient.getInstanceJsonById(instanceId);
      var title = instanceJson.get("title");
      var publications = instanceJson.get("publication");
      String publicationString = EMPTY;
      if (nonNull(publications) && publications.isArray() && !publications.isEmpty()) {
        publicationString = formatPublication(publications.get(0));
      }
      return title.asText() + publicationString;
    } catch (NotFoundException e) {
      var msg = "Instance not found by id=" + instanceId;
      log.error(msg);
      throw new BulkEditException(msg, ErrorType.WARNING);
    }
  }

  public String getInstanceTitleByHoldingsRecordId(String holdingsRecordId, String tenantId) {
    return ofNullable(self.getHoldingsRecordById(holdingsRecordId, tenantId))
        .map(holdingsRecord -> getInstanceTitleById(holdingsRecord.getInstanceId(), tenantId))
        .orElse(EMPTY);
  }

  private String formatPublication(JsonNode publication) {
    if (nonNull(publication)) {
      var publisher = publication.get("publisher");
      var dateOfPublication = publication.get("dateOfPublication");
      if (isNull(dateOfPublication) || dateOfPublication.isNull()) {
        return isNull(publisher) || publisher.isNull() ? EMPTY : String.format(". %s", publisher.asText());
      }
      return String.format(". %s, %s", isNull(publisher) || publisher.isNull() ? EMPTY : publisher.asText(), dateOfPublication.asText());
    }
    return EMPTY;
  }

  @Cacheable(cacheNames = "holdingsJsons")
  public JsonNode getHoldingsJsonById(String holdingsId, String tenantId) {
    try (var context = new FolioExecutionContextSetter(prepareContextForTenant(tenantId, folioModuleMetadata, folioExecutionContext))) {
      return holdingsStorageClient.getHoldingsJsonById(holdingsId);
    }
  }

  @Cacheable(cacheNames = "holdingsLocations")
  public JsonNode getHoldingsLocationById(String locationId, String tenantId) {
    if (ObjectUtils.isEmpty(locationId)) {
      return new ObjectMapper().createObjectNode();
    }
    try (var context = new FolioExecutionContextSetter(prepareContextForTenant(tenantId, folioModuleMetadata, folioExecutionContext))) {
      return locationClient.getLocationJsonById(locationId);
    }
  }

  public String getHoldingsData(String holdingsId, String tenantId) {
    if (ObjectUtils.isEmpty(holdingsId)) {
      return EMPTY;
    }
    var holdingsJson = self.getHoldingsJsonById(holdingsId, tenantId);
    var locationId = isNull(holdingsJson.get(PERMANENT_LOCATION_ID)) ? null : holdingsJson.get(PERMANENT_LOCATION_ID).asText();

    var locationJson = self.getHoldingsLocationById(locationId, tenantId);
    var activePrefix = nonNull(locationJson.get(IS_ACTIVE)) && locationJson.get(IS_ACTIVE).asBoolean() ? EMPTY : INACTIVE;
    var name = isNull(locationJson.get(NAME)) ? EMPTY : locationJson.get(NAME).asText();
    var locationName = activePrefix + name;

    var callNumber = Stream.of(holdingsJson.get(CALL_NUMBER_PREFIX), holdingsJson.get(CALL_NUMBER), holdingsJson.get(CALL_NUMBER_SUFFIX))
        .filter(Objects::nonNull)
        .map(JsonNode::asText)
        .collect(Collectors.joining(SPACE));

    return String.join(HOLDINGS_LOCATION_CALL_NUMBER_DELIMITER, locationName, callNumber);
  }

}
