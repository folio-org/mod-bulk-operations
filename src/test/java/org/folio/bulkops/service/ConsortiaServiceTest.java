package org.folio.bulkops.service;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import org.folio.bulkops.client.ConsortiaClient;
import org.folio.bulkops.client.ConsortiumClient;
import org.folio.bulkops.domain.bean.Consortia;
import org.folio.bulkops.domain.bean.ConsortiaCollection;
import org.folio.bulkops.domain.bean.UserTenant;
import org.folio.bulkops.domain.bean.UserTenantCollection;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.FolioModuleMetadata;
import org.folio.spring.integration.XOkapiHeaders;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ConsortiaServiceTest {

  @Mock
  private ConsortiaClient consortiaClient;
  @Mock
  private ConsortiumClient consortiumClient;
  @Mock
  private FolioExecutionContext folioExecutionContext;
  @Mock
  private FolioModuleMetadata folioModuleMetadata;

  @InjectMocks
  private ConsortiaService consortiaService;

  @Test
  void shouldReturnNothing_whenNoCentralTenantExist() {
    var userTenantCollection = new UserTenantCollection();
    userTenantCollection.setUserTenants(List.of());
    when(consortiaClient.getUserTenantCollection()).thenReturn(userTenantCollection);

    assertThat(consortiaService.getCentralTenantId("tenantId")).isEqualTo(EMPTY);
  }

  @Test
  void shouldReturnFirstUserTenant_whenThereAreUserTenants() {
    var userTenantCollection = new UserTenantCollection();
    var centralTenant = new UserTenant();
    centralTenant.setCentralTenantId("consortium");
    var otherUserTenant = new UserTenant();
    otherUserTenant.setCentralTenantId("college");
    userTenantCollection.setUserTenants(List.of(centralTenant, otherUserTenant));
    when(consortiaClient.getUserTenantCollection()).thenReturn(userTenantCollection);

    assertThat(consortiaService.getCentralTenantId("college")).isEqualTo("consortium");
  }

  @Test
  void shouldReturnAffiliatedTenants() {
    var consortia = new Consortia();
    consortia.setId("consortiaId");
    var consortiaCollection = new ConsortiaCollection();
    consortiaCollection.setConsortia(List.of(consortia));

    var userTenant = new UserTenant();
    userTenant.setCentralTenantId("centralTenantId");
    userTenant.setTenantId("memberTenantId");
    var userTenantCollection = new UserTenantCollection();
    userTenantCollection.setUserTenants(List.of(userTenant));

    when(consortiumClient.getConsortia()).thenReturn(consortiaCollection);
    when(consortiumClient.getConsortiaUserTenants("consortiaId", "userId", Integer.MAX_VALUE)).thenReturn(userTenantCollection);

    var expected = List.of("memberTenantId");
    var actual = consortiaService.getAffiliatedTenants("currentTenantId", "userId");

    assertEquals(expected, actual);
  }

  @Test
  void shouldReturnUserTenantsPerId() {
    var userTenantCollectionToGetCentral = new UserTenantCollection();
    var centralTenant = new UserTenant();
    centralTenant.setCentralTenantId("consortiaId");
    var otherUserTenant = new UserTenant();
    otherUserTenant.setCentralTenantId("memberTenantId");
    userTenantCollectionToGetCentral.setUserTenants(List.of(centralTenant, otherUserTenant));

    var consortia = new Consortia();
    consortia.setId("consortiaId");
    var consortiaCollection = new ConsortiaCollection();
    consortiaCollection.setConsortia(List.of(consortia));

    var userTenant = new UserTenant();
    userTenant.setCentralTenantId("centralTenantId");
    userTenant.setTenantId("memberTenantId");
    userTenant.setTenantName("memberTenantName");
    var userTenantCollection = new UserTenantCollection();
    userTenantCollection.setUserTenants(List.of(userTenant));

    HashMap<String, Collection<String>> headers = new HashMap<>();
    headers.put(XOkapiHeaders.TENANT, List.of("diku"));

    when(consortiaClient.getUserTenantCollection()).thenReturn(userTenantCollectionToGetCentral);
    when(consortiumClient.getConsortia()).thenReturn(consortiaCollection);
    when(consortiumClient.getConsortiaUserTenants("consortiaId", "userId", Integer.MAX_VALUE)).thenReturn(userTenantCollection);
    when(folioExecutionContext.getOkapiHeaders()).thenReturn(headers);

    var actual = consortiaService.getUserTenantsPerId("currentTenantId", "userId");

    assertTrue(actual.containsKey("memberTenantId"));
    var tenant = actual.get("memberTenantId");
    assertEquals("memberTenantName", tenant.getTenantName());
  }

}
