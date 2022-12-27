package org.folio.bulkops.adapters.impl.holdings;

import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.util.UUID;

import org.folio.bulkops.client.CallNumberTypeClient;
import org.folio.bulkops.client.HoldingsNoteTypeClient;
import org.folio.bulkops.client.HoldingsSourceClient;
import org.folio.bulkops.client.HoldingsTypeClient;
import org.folio.bulkops.client.IllPolicyClient;
import org.folio.bulkops.client.InstanceClient;
import org.folio.bulkops.client.LocationClient;
import org.folio.bulkops.client.StatisticalCodeClient;
import org.folio.bulkops.exception.NotFoundException;
import org.folio.bulkops.service.ErrorService;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@RequiredArgsConstructor
@Log4j2
public class HoldingsReferenceResolver {

  private final InstanceClient instanceClient;
  private final HoldingsTypeClient holdingsTypeClient;
  private final LocationClient locationClient;
  private final CallNumberTypeClient callNumberTypeClient;
  private final HoldingsNoteTypeClient holdingsNoteTypeClient;
  private final IllPolicyClient illPolicyClient;
  private final HoldingsSourceClient sourceClient;
  private final StatisticalCodeClient statisticalCodeClient;

  private final ErrorService errorService;

  public String getInstanceTitleById(String instanceId, UUID bulkOperationId, String identifier) {
    try {
      return isEmpty(instanceId) ? EMPTY
          : instanceClient.getById(instanceId)
            .getTitle();
    } catch (NotFoundException e) {
      var msg = "Instance not found by id=" + instanceId;
      log.error(msg);
      if (nonNull(bulkOperationId)) {
        errorService.saveError(bulkOperationId, identifier, msg);
      }
      return instanceId;
    }
  }

  @Cacheable(cacheNames = "holdingsTypesNames")
  public String getHoldingsTypeNameById(String id, UUID bulkOperationId, String identifier) {
    if (isEmpty(id)) {
      return EMPTY;
    }
    try {
      return isEmpty(id) ? EMPTY
          : holdingsTypeClient.getById(id)
            .getName();
    } catch (NotFoundException e) {
      var msg = "Holdings type not found by id=" + id;
      log.error(msg);
      if (nonNull(bulkOperationId)) {
        errorService.saveError(bulkOperationId, identifier, msg);
      }
      return id;
    }
  }

  @Cacheable(cacheNames = "holdingsLocationsNames")
  public String getLocationNameById(String id, UUID bulkOperationId, String identifier) {
    try {
      return isEmpty(id) ? EMPTY
          : locationClient.getLocationById(id)
            .getName();
    } catch (NotFoundException e) {
      var msg = "Location not found by id=" + id;
      log.error(msg);
      if (nonNull(bulkOperationId)) {
        errorService.saveError(bulkOperationId, identifier, msg);
      }
      return id;
    }
  }

  @Cacheable(cacheNames = "holdingsCallNumberTypesNames")
  public String getCallNumberTypeNameById(String id, UUID bulkOperationId, String identifier) {
    try {
      return isEmpty(id) ? EMPTY
          : callNumberTypeClient.getById(id)
            .getName();
    } catch (NotFoundException e) {
      var msg = "Call number type not found by id=" + id;
      log.error(msg);
      if (nonNull(bulkOperationId)) {
        errorService.saveError(bulkOperationId, identifier, msg);
      }
      return id;
    }
  }

  @Cacheable(cacheNames = "holdingsNoteTypesNames")
  public String getNoteTypeNameById(String id, UUID bulkOperationId, String identifier) {
    try {
      return isEmpty(id) ? EMPTY
          : holdingsNoteTypeClient.getById(id)
            .getName();
    } catch (NotFoundException e) {
      var msg = String.format("Note type not found by id=[%s]", id);
      log.error(msg);
      if (nonNull(bulkOperationId)) {
        errorService.saveError(bulkOperationId, identifier, msg);
      }
      return id;
    }
  }

  @Cacheable(cacheNames = "illPolicyNames")
  public String getIllPolicyNameById(String id, UUID bulkOperationId, String identifier) {
    try {
      return isEmpty(id) ? EMPTY
          : illPolicyClient.getById(id)
            .getName();
    } catch (NotFoundException e) {
      var msg = String.format("Ill policy not found by id=[%s]", id);
      log.error(msg);
      if (nonNull(bulkOperationId)) {
        errorService.saveError(bulkOperationId, identifier, msg);
      }
      return id;
    }
  }

  @Cacheable(cacheNames = "holdingsSourceNames")
  public String getSourceNameById(String id, UUID bulkOperationId, String identifier) {
    try {
      return isEmpty(id) ? EMPTY
          : sourceClient.getById(id)
            .getName();
    } catch (NotFoundException e) {
      var msg = String.format("Holdings record source not found by id=[%s]", id);
      log.error(msg);
      if (nonNull(bulkOperationId)) {
        errorService.saveError(bulkOperationId, identifier, msg);
      }
      return id;
    }
  }

  @Cacheable(cacheNames = "holdingsStatisticalCodeNames")
  public String getStatisticalCodeNameById(String id, UUID bulkOperationId, String identifier) {
    try {
      return isEmpty(id) ? EMPTY
          : statisticalCodeClient.getById(id)
            .getName();
    } catch (NotFoundException e) {
      var msg = String.format("Statistical code not found by id=[%s]", id);
      log.error(msg);
      if (nonNull(bulkOperationId)) {
        errorService.saveError(bulkOperationId, identifier, msg);
      }
      return id;
    }
  }
}
