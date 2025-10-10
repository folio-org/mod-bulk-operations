package org.folio.bulkops.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.bulkops.domain.bean.DamagedStatus;
import org.folio.bulkops.domain.bean.ItemLocation;
import org.folio.bulkops.domain.bean.LoanType;
import org.folio.bulkops.domain.bean.MaterialType;
import org.folio.bulkops.domain.bean.ServicePoint;
import org.folio.bulkops.domain.bean.StatisticalCode;
import org.folio.bulkops.domain.bean.StatisticalCodeType;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;

@Service
@Log4j2
@RequiredArgsConstructor
public class ItemReferenceHelper implements InitializingBean {
  private final ItemReferenceService itemReferenceService;

  public String getCallNumberTypeNameById(String callNumberTypeId) {
    return itemReferenceService.getCallNumberTypeNameById(callNumberTypeId);
  }

  public DamagedStatus getDamagedStatusById(String damagedStatusId) {
    return itemReferenceService.getDamagedStatusById(damagedStatusId);
  }

  public DamagedStatus getDamagedStatusByName(String name) {
    return itemReferenceService.getDamagedStatusByName(name);
  }

  public String getNoteTypeNameById(String noteTypeId, String tenantId) {
    return itemReferenceService.getNoteTypeNameById(noteTypeId, tenantId);
  }

  public String getNoteTypeIdByName(String name) {
    return itemReferenceService.getNoteTypeIdByName(name);
  }

  public ServicePoint getServicePointById(String servicePointId) {
    return itemReferenceService.getServicePointById(servicePointId);
  }

  public ServicePoint getServicePointByName(String name) {
    return itemReferenceService.getServicePointByName(name);
  }

  public StatisticalCode getStatisticalCodeById(String statisticalCodeId) {
    return itemReferenceService.getStatisticalCodeById(statisticalCodeId);
  }

  public StatisticalCodeType getStatisticalCodeTypeById(String id) {
    return itemReferenceService.getStatisticalCodeTypeById(id);
  }

  public String getStatisticalCodeIdByCode(String code) {
    return itemReferenceService.getStatisticalCodeIdByCode(code);
  }

  public String getUserNameById(String userId) {
    return itemReferenceService.getUserNameById(userId);
  }

  public String getUserIdByUserName(String name) {
    return itemReferenceService.getUserIdByUserName(name);
  }

  public ItemLocation getLocationByName(String name) {
    return itemReferenceService.getLocationByName(name);
  }

  public MaterialType getMaterialTypeByName(String name) {
    return itemReferenceService.getMaterialTypeByName(name);
  }

  public LoanType getLoanTypeByName(String name) {
    return itemReferenceService.getLoanTypeByName(name);
  }

  public LoanType getLoanTypeById(String id, String tenantId) {
    return itemReferenceService.getLoanTypeById(id, tenantId);
  }

  public MaterialType getMaterialTypeById(String id, String tenantId) {
    return itemReferenceService.getMaterialTypeById(id, tenantId);
  }

  public ItemLocation getLocationById(String id, String tenantId) {
    return itemReferenceService.getLocationById(id, tenantId);
  }

  private static ItemReferenceHelper service;

  @Override
  public void afterPropertiesSet() {
    service = this;
  }

  public static ItemReferenceHelper service() {
    return service;
  }
}
