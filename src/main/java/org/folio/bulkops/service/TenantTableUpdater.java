package org.folio.bulkops.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.bulkops.domain.bean.BulkOperationsEntity;
import org.folio.bulkops.domain.bean.HoldingsRecord;
import org.folio.bulkops.domain.bean.Item;
import org.folio.bulkops.domain.dto.UnifiedTable;
import org.folio.spring.FolioExecutionContext;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
@RequiredArgsConstructor
@Log4j2
public class TenantTableUpdater {

  public static final String TENANT_VALUE_IN_CONSORTIA_FOR_MEMBER = "Member";

  private final FolioExecutionContext folioExecutionContext;
  private final ConsortiaService consortiaService;

  public void updateTenantInHeadersAndRows(UnifiedTable unifiedTable, Class<? extends BulkOperationsEntity> clazz) {
    if (!(clazz == Item.class || clazz == HoldingsRecord.class)) {
      return;
    }
    int tenantPosition = unifiedTable.getHeader().size() - 1;
    if (isNeedUpdateTablePreview()) {
      var userTenants = consortiaService.getUserTenantsPerId(folioExecutionContext.getTenantId(), folioExecutionContext.getUserId().toString());
      var header = unifiedTable.getHeader().get(tenantPosition);
      header.setValue(TENANT_VALUE_IN_CONSORTIA_FOR_MEMBER);
      var rows = unifiedTable.getRows();
      rows.forEach(row -> {
        int last = row.getRow().size() - 1;
        var tenantId = row.getRow().get(last);
        var tenant = userTenants.get(tenantId);
        if (Objects.nonNull(tenant)) {
          row.getRow().set(last, tenant.getTenantName());
        }
      });
    } else {
      var headers = unifiedTable.getHeader();
      headers.remove(tenantPosition);
      unifiedTable.getRows().forEach(row -> row.getRow().remove(tenantPosition));
    }
  }

  private boolean isNeedUpdateTablePreview() {
    return consortiaService.isCurrentTenantInConsortia(folioExecutionContext.getTenantId());
  }
}
