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
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.ObjectUtils;
import org.folio.bulkops.client.InstanceClient;
import org.folio.bulkops.client.InstanceStorageClient;
import org.folio.bulkops.client.ItemClient;
import org.folio.bulkops.client.LocationClient;
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
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Log4j2
public class HoldingsReferenceService {

  private final LocationClient locationClient;
  private final InstanceClient instanceClient;
  private final InstanceStorageClient instanceStorageClient;
  private final ItemClient itemClient;
  private final FolioModuleMetadata folioModuleMetadata;
  private final FolioExecutionContext folioExecutionContext;
  private final HoldingsReferenceCacheService holdingsReferenceCacheService;

  public HoldingsRecord getHoldingsRecordById(String id, String tenantId) {
    return holdingsReferenceCacheService.getHoldingsRecordById(id, tenantId);
  }

  public HoldingsType getHoldingsTypeById(String id) {
    return holdingsReferenceCacheService.getHoldingsTypeById(id);
  }

  public HoldingsType getHoldingsTypeByName(String name, String tenantId) {
    return holdingsReferenceCacheService.getHoldingsTypeByName(name, tenantId);
  }

  public ItemLocation getLocationById(String id) {
    return holdingsReferenceCacheService.getLocationById(id);
  }

  public ItemLocation getLocationIdByName(String name) {
    var locations = locationClient.getByQuery(format(QUERY_PATTERN_NAME, encode(name)));
    if (locations.getLocations().isEmpty()) {
      throw new ReferenceDataNotFoundException(format("Location not found by name=%s", name));
    }
    return locations.getLocations().getFirst();
  }

  public String getCallNumberTypeNameById(String id) {
   return holdingsReferenceCacheService.getCallNumberTypeNameById(id);
  }

  public String getCallNumberTypeIdByName(String name, String tenantId) {
   return holdingsReferenceCacheService.getCallNumberTypeIdByName(name, tenantId);
  }

  public String getNoteTypeNameById(String id, String tenantId) {
    return holdingsReferenceCacheService.getNoteTypeNameById(id, tenantId);
  }

  public String getNoteTypeIdByName(String name, String tenantId) {
    return holdingsReferenceCacheService.getNoteTypeIdByName(name, tenantId);
  }

  public IllPolicy getIllPolicyById(String id) {
    return holdingsReferenceCacheService.getIllPolicyById(id);
  }

  public IllPolicy getIllPolicyByName(String name, String tenantId) {
    return holdingsReferenceCacheService.getIllPolicyByName(name, tenantId);
  }

  public HoldingsRecordsSource getSourceById(String id) {
    return holdingsReferenceCacheService.getSourceById(id);
  }

  public HoldingsRecordsSource getSourceByName(String name, String tenantId) {
    return holdingsReferenceCacheService.getSourceByName(name, tenantId);
  }

  public StatisticalCode getStatisticalCodeById(String id) {
    return holdingsReferenceCacheService.getStatisticalCodeById(id);
  }

  public StatisticalCode getStatisticalCodeByName(String name, String tenantId) {
    return holdingsReferenceCacheService.getStatisticalCodeByName(name, tenantId);
  }

  public List<HoldingsNoteType> getAllHoldingsNoteTypes(String tenantId) {
    return holdingsReferenceCacheService.getAllHoldingsNoteTypes(tenantId);
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
    return ofNullable(holdingsReferenceCacheService.getHoldingsRecordById(holdingsRecordId, tenantId))
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

  public JsonNode getHoldingsJsonById(String holdingsId, String tenantId) {
    return holdingsReferenceCacheService.getHoldingsJsonById(holdingsId, tenantId);
  }

  public JsonNode getHoldingsLocationById(String locationId, String tenantId) {
    return holdingsReferenceCacheService.getHoldingsLocationById(locationId, tenantId);
  }

  public String getHoldingsData(String holdingsId, String tenantId) {
    if (ObjectUtils.isEmpty(holdingsId)) {
      return EMPTY;
    }
    var holdingsJson = holdingsReferenceCacheService.getHoldingsJsonById(holdingsId, tenantId);
    var locationId = isNull(holdingsJson.get(PERMANENT_LOCATION_ID)) ? null : holdingsJson.get(PERMANENT_LOCATION_ID).asText();

    var locationJson = holdingsReferenceCacheService.getHoldingsLocationById(locationId, tenantId);
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
