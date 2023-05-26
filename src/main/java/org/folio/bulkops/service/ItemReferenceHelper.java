package org.folio.bulkops.service;

import org.folio.bulkops.domain.bean.*;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

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

  public String getNoteTypeNameById(String noteTypeId) {
    return itemReferenceService.getNoteTypeNameById(noteTypeId);
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

  public String getStatisticalCodeById(String statisticalCodeId) {
    return itemReferenceService.getStatisticalCodeById(statisticalCodeId);
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

  private static ItemReferenceHelper service;

  @Override
  public void afterPropertiesSet() {
    service = this;
  }

  public static ItemReferenceHelper service() {
    return service;
  }
}
