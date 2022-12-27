package org.folio.bulkops.adapters.impl.items;

import static org.apache.commons.lang3.ObjectUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.EMPTY;

import java.util.Objects;
import java.util.UUID;

import org.folio.bulkops.client.CallNumberTypeClient;
import org.folio.bulkops.client.DamagedStatusClient;
import org.folio.bulkops.client.ItemNoteTypeClient;
import org.folio.bulkops.client.ServicePointClient;
import org.folio.bulkops.client.StatisticalCodeClient;
import org.folio.bulkops.client.UserClient;
import org.folio.bulkops.error.NotFoundException;
import org.folio.bulkops.service.ErrorService;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Component
@Log4j2
@RequiredArgsConstructor
public class ItemReferenceResolver {

  private final CallNumberTypeClient callNumberTypeClient;
  private final DamagedStatusClient damagedStatusClient;
  private final ItemNoteTypeClient itemNoteTypeClient;
  private final ServicePointClient servicePointClient;
  private final StatisticalCodeClient statisticalCodeClient;
  private final UserClient userClient;
  private final ErrorService errorService;

  @Cacheable(cacheNames = "callNumberTypeNames")
  public String getCallNumberTypeNameById(String callNumberTypeId, UUID bulkOperationId, String identifier) {
    try {
      return isEmpty(callNumberTypeId) ? EMPTY
          : callNumberTypeClient.getById(callNumberTypeId)
            .getName();
    } catch (NotFoundException e) {
      var msg = String.format("Call number type was not found by id:: [%s]", callNumberTypeId);
      log.error(msg);
      if (Objects.nonNull(bulkOperationId)) {
        errorService.saveError(bulkOperationId, identifier, msg);
      }
      return callNumberTypeId;
    }
  }

  @Cacheable(cacheNames = "damagedStatusNames")
  public String getDamagedStatusNameById(String damagedStatusId, UUID bulkOperationId, String identifier) {
    try {
      return isEmpty(damagedStatusId) ? EMPTY
          : damagedStatusClient.getById(damagedStatusId)
            .getName();
    } catch (NotFoundException e) {
      var msg = String.format("Damaged status was not found by id: [%s]", damagedStatusId);
      log.error(msg);
      if (Objects.nonNull(bulkOperationId)) {
        errorService.saveError(bulkOperationId, identifier, msg);
      }
      return damagedStatusId;
    }
  }

  @Cacheable(cacheNames = "noteTypeNames")
  public String getNoteTypeNameById(String noteTypeId, UUID bulkOperationId, String identifier) {
    try {
      return isEmpty(noteTypeId) ? EMPTY
          : itemNoteTypeClient.getById(noteTypeId)
            .getName();
    } catch (NotFoundException e) {
      var msg = String.format("Note type was not found by id: [%s]", noteTypeId);
      log.error(msg);
      if (Objects.nonNull(bulkOperationId)) {
        errorService.saveError(bulkOperationId, identifier, msg);
      }
      return noteTypeId;
    }
  }

  @Cacheable(cacheNames = "servicePointNames")
  public String getServicePointNameById(String servicePointId, UUID bulkOperationId, String identifier) {
    try {
      return isEmpty(servicePointId) ? EMPTY
          : servicePointClient.getById(servicePointId)
            .getName();
    } catch (NotFoundException e) {
      var msg = String.format("Service point was not found by id: [%s]", servicePointId);
      log.error(msg);
      if (Objects.nonNull(bulkOperationId)) {
        errorService.saveError(bulkOperationId, identifier, msg);
      }
      return servicePointId;
    }
  }

  @Cacheable(cacheNames = "statisticalCodeNames")
  public String getStatisticalCodeById(String statisticalCodeId, UUID bulkOperationId, String identifier) {
    try {
      return isEmpty(statisticalCodeId) ? EMPTY
          : statisticalCodeClient.getById(statisticalCodeId)
            .getCode();
    } catch (NotFoundException e) {
      var msg = String.format("Statistical code was not found by id: [%s]", statisticalCodeId);
      log.error(msg);
      if (Objects.nonNull(bulkOperationId)) {
        errorService.saveError(bulkOperationId, identifier, msg);
      }
      return statisticalCodeId;
    }
  }

  @Cacheable(cacheNames = "userNames")
  public String getUserNameById(String userId, UUID bulkOperationId, String identifier) {
    try {
      return isEmpty(userId) ? EMPTY
          : userClient.getUserById(userId)
            .getUsername();
    } catch (NotFoundException e) {
      var msg = String.format("User name was not found by id: [%s]", userId);
      log.error(msg);
      if (Objects.nonNull(bulkOperationId)) {
        errorService.saveError(bulkOperationId, identifier, msg);
      }
      return userId;
    }
  }
}
