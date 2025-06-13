package org.folio.bulkops.service;

import static java.lang.String.format;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.folio.bulkops.util.Constants.QUERY_PATTERN_BARCODE;
import static org.folio.bulkops.util.Constants.QUERY_PATTERN_HRID;
import static org.folio.bulkops.util.Constants.QUERY_PATTERN_NAME;
import static org.folio.bulkops.util.FolioExecutionContextUtil.prepareContextForTenant;
import static org.folio.bulkops.util.Utils.encode;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.ObjectUtils;
import org.folio.bulkops.client.CallNumberTypeClient;
import org.folio.bulkops.client.HoldingsClient;
import org.folio.bulkops.client.HoldingsNoteTypeClient;
import org.folio.bulkops.client.HoldingsSourceClient;
import org.folio.bulkops.client.HoldingsTypeClient;
import org.folio.bulkops.client.IllPolicyClient;
import org.folio.bulkops.client.InstanceClient;
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
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Log4j2
public class HoldingsReferenceService {

  private final HoldingsClient holdingsClient;
  private final HoldingsTypeClient holdingsTypeClient;
  private final LocationClient locationClient;
  private final CallNumberTypeClient callNumberTypeClient;
  private final HoldingsNoteTypeClient holdingsNoteTypeClient;
  private final IllPolicyClient illPolicyClient;
  private final HoldingsSourceClient holdingsSourceClient;
  private final StatisticalCodeClient statisticalCodeClient;
  private final InstanceClient instanceClient;
  private final ItemClient itemClient;
  private final FolioModuleMetadata folioModuleMetadata;
  private final FolioExecutionContext folioExecutionContext;

  @Cacheable(cacheNames = "holdings")
  public HoldingsRecord getHoldingsRecordById(String id, String tenantId) {
    return holdingsClient.getHoldingById(id);
  }

  @Cacheable(cacheNames = "holdingsTypesNames")
  public HoldingsType getHoldingsTypeById(String id, String tenantId) {
    try {
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
    return holdingsTypes.getHoldingsTypes().get(0);
  }

  @Cacheable(cacheNames = "holdingsLocationsNames")
  public ItemLocation getLocationById(String id, String tenantId) {
    try {
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
    return locations.getLocations().get(0);
  }

  @Cacheable(cacheNames = "holdingsCallNumberTypesNames")
  public String getCallNumberTypeNameById(String id, String tenantId) {
    try {
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
    return callNumberTypes.getCallNumberTypes().get(0).getId();
  }

  @Cacheable(cacheNames = "holdingsNoteTypesNames")
  public String getNoteTypeNameById(String id, String tenantId) {
    try {
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
    return noteTypes.getHoldingsNoteTypes().get(0).getId();
  }

  @Cacheable(cacheNames = "illPolicyNames")
  public IllPolicy getIllPolicyById(String id, String tenantId) {
    try {
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
    return illPolicies.getIllPolicies().get(0);
  }

  @Cacheable(cacheNames = "holdingsSourceNames")
  public HoldingsRecordsSource getSourceById(String id, String tenantId) {
    try {
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
    return sources.getHoldingsRecordsSources().get(0);
  }

  @Cacheable(cacheNames = "holdingsStatisticalCodeNames")
  public StatisticalCode getStatisticalCodeById(String id, String tenantId) {
    try {
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
    return statisticalCodes.getStatisticalCodes().get(0);
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
    return items.getItems().get(0).getHoldingsRecordId();
  }

  public String getInstanceTitleById(String instanceId, String tenantId) {
    if (isEmpty(instanceId)) {
      return EMPTY;
    }
    try (var context = new FolioExecutionContextSetter(prepareContextForTenant(tenantId, folioModuleMetadata, folioExecutionContext))) {
      var instanceJson = instanceClient.getInstanceJsonById(instanceId);
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

  private String formatPublication(JsonNode publication) {
    if (nonNull(publication)) {
      var publisher = publication.get("publisher");
      var dateOfPublication = publication.get("dateOfPublication");
      if (isNull(dateOfPublication)) {
        return isNull(publisher) ? EMPTY : String.format(". %s", publisher.asText());
      }
      return String.format(". %s, %s", isNull(publisher) ? EMPTY : publisher.asText(), dateOfPublication.asText());
    }
    return EMPTY;
  }

  @Cacheable(cacheNames = "holdingsJsons")
  public JsonNode getHoldingsJsonById(String holdingsId, String tenantId) {
    try (var context = new FolioExecutionContextSetter(prepareContextForTenant(tenantId, folioModuleMetadata, folioExecutionContext))) {
      return holdingsClient.getHoldingsJsonById(holdingsId);
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
}
