package org.folio.bulkops.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.bulkops.domain.bean.BulkOperationsEntity;
import org.folio.bulkops.domain.bean.HoldingsRecord;
import org.folio.bulkops.domain.bean.Item;
import org.folio.bulkops.domain.dto.UnifiedTable;
import org.folio.spring.FolioExecutionContext;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Log4j2
public class TenantTableUpdater {

  public static final String TENANT_VALUE_IN_CONSORTIA_FOR_MEMBER = "Member";

  private final FolioExecutionContext folioExecutionContext;
  private final ConsortiaService consortiaService;

  public void updateTenantHeaderAndRow(UnifiedTable unifiedTable, Class<? extends BulkOperationsEntity> clazz) {
    if (!(clazz == Item.class || clazz == HoldingsRecord.class)) {
      return;
    }
    int tenantPosition = unifiedTable.getHeader().size() - 1;
    if (consortiaService.isCurrentTenantCentralTenant(folioExecutionContext.getTenantId())) {
      var header = unifiedTable.getHeader().get(tenantPosition);
      header.setValue(TENANT_VALUE_IN_CONSORTIA_FOR_MEMBER);
    } else {
      var headers = unifiedTable.getHeader();
      headers.remove(tenantPosition);
      unifiedTable.getRows().forEach(row -> row.getRow().remove(tenantPosition));
    }
  }
}
