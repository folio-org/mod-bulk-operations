package org.folio.bulkops.util;

import org.folio.bulkops.domain.dto.Action;
import org.folio.spring.FolioExecutionContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RuleHttpEncoderTest {

  private static final String CURRENT_TENANT = "current_tenant";
  private static final String TENANT_FROM_UI = "tenant_from_ui";

  @Mock
  private FolioExecutionContext folioExecutionContext;

  @Test
  void getTenantFromAction_whenNeedToUseCurrentTenantTest() {
    when(folioExecutionContext.getTenantId()).thenReturn(CURRENT_TENANT);
    assertEquals(CURRENT_TENANT, RuleUtils.getTenantFromAction(new Action(), folioExecutionContext));
    assertEquals(CURRENT_TENANT, RuleUtils.getTenantFromAction(new Action().updatedTenants(List.of()), folioExecutionContext));
    assertEquals(CURRENT_TENANT, RuleUtils.getTenantFromAction(new Action().updatedTenants(List.of()).tenants(List.of()), folioExecutionContext));
    assertEquals(CURRENT_TENANT, RuleUtils.getTenantFromAction(new Action().tenants(List.of()), folioExecutionContext));
  }

  @Test
  void getTenantFromAction_whenNeedToUseTenantFromUITest() {
    assertEquals(TENANT_FROM_UI, RuleUtils.getTenantFromAction(new Action().updatedTenants(List.of()).tenants(List.of(TENANT_FROM_UI)), folioExecutionContext));
    assertEquals(TENANT_FROM_UI, RuleUtils.getTenantFromAction(new Action().updatedTenants(List.of(TENANT_FROM_UI)).tenants(List.of(TENANT_FROM_UI)), folioExecutionContext));
    assertEquals(TENANT_FROM_UI, RuleUtils.getTenantFromAction(new Action().updatedTenants(List.of(TENANT_FROM_UI)).tenants(List.of()), folioExecutionContext));
    assertEquals(TENANT_FROM_UI, RuleUtils.getTenantFromAction(new Action().tenants(List.of(TENANT_FROM_UI)), folioExecutionContext));
    assertEquals(TENANT_FROM_UI, RuleUtils.getTenantFromAction(new Action().updatedTenants(List.of(TENANT_FROM_UI)), folioExecutionContext));
  }
}
