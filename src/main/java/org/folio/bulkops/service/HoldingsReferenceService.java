package org.folio.bulkops.service;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isEmpty;

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
import org.folio.bulkops.client.LocationClient;
import org.folio.bulkops.client.StatisticalCodeClient;
import org.folio.bulkops.domain.bean.HoldingsRecord;
import org.folio.bulkops.domain.bean.HoldingsRecordCollection;
import org.folio.bulkops.domain.bean.HoldingsRecordsSource;
import org.folio.bulkops.exception.NotFoundException;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Log4j2
public class HoldingsReferenceService {
  private static final String QUERY_PATTERN_NAME = "name==\"%s\"";

  private final InstanceClient instanceClient;
  private final HoldingsClient holdingsClient;
  private final HoldingsTypeClient holdingsTypeClient;
  private final LocationClient locationClient;
  private final CallNumberTypeClient callNumberTypeClient;
  private final HoldingsNoteTypeClient holdingsNoteTypeClient;
  private final IllPolicyClient illPolicyClient;
  private final HoldingsSourceClient holdingsSourceClient;
  private final StatisticalCodeClient statisticalCodeClient;

  public HoldingsRecord getHoldingsRecordById(String id) {
    return holdingsClient.getHoldingById(id);
  }

  public HoldingsRecordCollection getHoldingsByQuery(String query, long offset, long limit) {
    return holdingsClient.getHoldingsByQuery(query, offset, limit);
  }

  public String getInstanceTitleById(String id) {
    try {
      return isEmpty(id) ? EMPTY
        : instanceClient.getById(id)
          .getTitle();
    } catch (NotFoundException e) {
      log.error("Instance not found by id={}", id);
      return id;
    }
  }

  @Cacheable(cacheNames = "holdingsTypesNames")
  public String getHoldingsTypeNameById(String id) {
    try {
      return isEmpty(id) ? EMPTY
        : holdingsTypeClient.getById(id)
          .getName();
    } catch (NotFoundException e) {
      log.error("Holdings type not found by id={}", id);
      return id;
    }
  }

  @Cacheable(cacheNames = "holdingsTypeIds")
  public String getHoldingsTypeIdByName(String name) {
    if (isEmpty(name)) {
      return null;
    }
    var holdingsTypes = holdingsTypeClient.getByQuery(String.format(QUERY_PATTERN_NAME, name));
    if (holdingsTypes.getHoldingsTypes().isEmpty()) {
      log.error("Holdings type not found by name={}", name);
      return name;
    }
    return holdingsTypes.getHoldingsTypes().get(0).getId();
  }

  @Cacheable(cacheNames = "holdingsLocationsNames")
  public String getLocationNameById(String id) {
    try {
      return isEmpty(id) ? EMPTY
        : locationClient.getLocationById(id)
          .getName();
    } catch (NotFoundException e) {
      log.error("Location not found by id={}", id);
      return id;
    }
  }

  public String getLocationIdByName(String name) {
    if (isEmpty(name)) {
      return null;
    }
    var locations = locationClient.getLocationByQuery(String.format(QUERY_PATTERN_NAME, name));
    if (locations.getLocations().isEmpty()) {
      log.error("Location not found by name={}", name);
      return name;
    }
    return locations.getLocations().get(0).getId();
  }

  @Cacheable(cacheNames = "holdingsCallNumberTypesNames")
  public String getCallNumberTypeNameById(String id) {
    try {
      return isEmpty(id) ? EMPTY
        : callNumberTypeClient.getById(id)
          .getName();
    } catch (NotFoundException e) {
      log.error("Call number type not found by id={}", id);
      return id;
    }
  }

  @Cacheable(cacheNames = "holdingsCallNumberTypes")
  public String getCallNumberTypeIdByName(String name) {
    if (isEmpty(name)) {
      return null;
    }
    var callNumberTypes = callNumberTypeClient.getByQuery(String.format(QUERY_PATTERN_NAME, name));
    if (callNumberTypes.getCallNumberTypes().isEmpty()) {
      log.error("Call number type not found by name={}", name);
      return name;
    }
    return callNumberTypes.getCallNumberTypes().get(0).getId();
  }

  @Cacheable(cacheNames = "holdingsNoteTypesNames")
  public String getNoteTypeNameById(String id) {
    try {
      return isEmpty(id) ? EMPTY
        : holdingsNoteTypeClient.getById(id)
          .getName();
    } catch (NotFoundException e) {
      log.error("Note type not found by id={}", id);
      return id;
    }
  }

  @Cacheable(cacheNames = "holdingsNoteTypes")
  public String getNoteTypeIdByName(String name) {
    if (isEmpty(name)) {
      return null;
    }
    var noteTypes = holdingsNoteTypeClient.getByQuery(String.format(QUERY_PATTERN_NAME, name));
    if (noteTypes.getHoldingsNoteTypes().isEmpty()) {
      log.error("Note type not found by name={}", name);
      return name;
    }
    return noteTypes.getHoldingsNoteTypes().get(0).getId();
  }

  @Cacheable(cacheNames = "illPolicyNames")
  public String getIllPolicyNameById(String id) {
    try {
      return isEmpty(id) ? EMPTY
        : illPolicyClient.getById(id)
          .getName();
    } catch (NotFoundException e) {
      log.error("Ill policy not found by id={}", id);
      return id;
    }
  }

  @Cacheable(cacheNames = "illPolicies")
  public String getIllPolicyIdByName(String name) {
    if (isEmpty(name)) {
      return null;
    }
    var illPolicies = illPolicyClient.getByQuery(String.format(QUERY_PATTERN_NAME, name));
    if (illPolicies.getIllPolicies().isEmpty()) {
      log.error("Ill policy not found by name={}", name);
      return name;
    }
    return illPolicies.getIllPolicies().get(0).getId();
  }

  @Cacheable(cacheNames = "sources")
  public HoldingsRecordsSource getSourceById(String id) {
    return holdingsSourceClient.getById(id);
  }

  @Cacheable(cacheNames = "holdingsSourceNames")
  public String getSourceNameById(String id) {
    try {
      return isEmpty(id) ? EMPTY
        : holdingsSourceClient.getById(id)
          .getName();
    } catch (NotFoundException e) {
      log.error("Holdings record source not found by id={}", id);
      return id;
    }
  }

  @Cacheable(cacheNames = "holdingsSources")
  public String getSourceIdByName(String name) {
    var sources = holdingsSourceClient.getByQuery(String.format(QUERY_PATTERN_NAME, name));
    if (ObjectUtils.isEmpty(sources) || ObjectUtils.isEmpty(sources.getHoldingsRecordsSources())) {
      log.error("Source not found by name={}", name);
      return EMPTY;
    }
    return sources.getHoldingsRecordsSources().get(0).getId();
  }

  @Cacheable(cacheNames = "holdingsStatisticalCodeNames")
  public String getStatisticalCodeNameById(String id) {
    try {
      return isEmpty(id) ? EMPTY
        : statisticalCodeClient.getById(id)
          .getName();
    } catch (NotFoundException e) {
      log.error("Statistical code not found by id={}", id);
      return id;
    }
  }

  @Cacheable(cacheNames = "holdingsStatisticalCodes")
  public String getStatisticalCodeIdByName(String name) {
    var statisticalCodes = statisticalCodeClient.getByQuery(String.format(QUERY_PATTERN_NAME, name));
    if (statisticalCodes.getStatisticalCodes().isEmpty()) {
      log.error("Statistical code not found by name={}", name);
      return name;
    }
    return statisticalCodes.getStatisticalCodes().get(0).getId();
  }
}
