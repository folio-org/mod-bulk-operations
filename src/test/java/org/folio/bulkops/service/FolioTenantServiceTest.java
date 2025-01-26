package org.folio.bulkops.service;

import org.folio.spring.service.PrepareSystemUserService;
import org.folio.tenant.domain.dto.TenantAttributes;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FolioTenantServiceTest {

  @InjectMocks
  FolioTenantService folioTenantService;

  @Mock
  PrepareSystemUserService prepareSystemUserService;

  @Test
  void shouldProcessAfterTenantUpdating() {
    TenantAttributes tenantAttributes = createTenantAttributes();

    doNothing().when(prepareSystemUserService)
      .setupSystemUser();

    folioTenantService.afterTenantUpdate(tenantAttributes);

    verify(prepareSystemUserService).setupSystemUser();
  }

  @Test
  void shouldFailAfterTenantUpdating() {
    TenantAttributes tenantAttributes = createTenantAttributes();

    doThrow(NullPointerException.class).when(prepareSystemUserService)
      .setupSystemUser();

    assertThrows(NullPointerException.class, () -> folioTenantService.afterTenantUpdate(tenantAttributes));
  }

  private TenantAttributes createTenantAttributes() {
    TenantAttributes tenantAttributes = new TenantAttributes();
    tenantAttributes.setPurge(false);
    tenantAttributes.setModuleTo("mod-bulk-operations");
    return tenantAttributes;
  }
}
