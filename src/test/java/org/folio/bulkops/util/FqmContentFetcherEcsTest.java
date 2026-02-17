package org.folio.bulkops.util;

import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.folio.bulkops.BaseTest;
import org.folio.bulkops.client.QueryClient;
import org.folio.bulkops.client.SearchClient;
import org.folio.bulkops.domain.dto.ConsortiumItem;
import org.folio.bulkops.domain.dto.ConsortiumItemCollection;
import org.folio.bulkops.domain.dto.EntityType;
import org.folio.bulkops.service.ConsortiaService;
import org.folio.bulkops.service.EntityTypeService;
import org.folio.querytool.domain.dto.ContentsRequest;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.client.AuthnClient;
import org.folio.spring.client.PermissionsClient;
import org.folio.spring.client.UsersClient;
import org.folio.spring.service.SystemUserService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@ContextConfiguration(initializers = BaseTest.Initializer.class)
class FqmContentFetcherEcsTest {

  @MockitoBean private ConsortiaService consortiaService;
  @MockitoBean private EntityTypeService entityTypeService;
  @MockitoBean private FolioExecutionContext folioExecutionContext;
  @MockitoBean private QueryClient queryClient;
  @MockitoBean public SystemUserService systemUserService;
  @MockitoBean public AuthnClient authnClient;
  @MockitoBean public UsersClient usersClient;
  @MockitoBean public PermissionsClient permissionsClient;
  @MockitoBean private SearchClient searchClient;

  @Value("${application.fqm-fetcher.max_chunk_size}")
  private int chunkSize;

  @Autowired private FqmContentFetcher fqmContentFetcher;

  @Test
  void testLocalInstanceInLocalTenant() {
    String tenantId = "local";
    String centralTenantId = "";
    mockCommon(tenantId, centralTenantId);

    UUID uuid = UUID.randomUUID();

    try (var is =
        fqmContentFetcher.contents(
            List.of(uuid), EntityType.INSTANCE, List.of(), UUID.randomUUID())) {
      ArgumentCaptor<ContentsRequest> captor = ArgumentCaptor.forClass(ContentsRequest.class);
      await()
          .atMost(2, TimeUnit.SECONDS)
          .untilAsserted(() -> verify(queryClient, atLeastOnce()).getContents(captor.capture()));

      ContentsRequest req = captor.getValue();
      List<List<String>> ids = req.getIds();
      Assertions.assertEquals(1, ids.size());
      Assertions.assertEquals(List.of(uuid.toString()), ids.getFirst());
    } catch (IOException e) {
      Assertions.fail("Fail reading content");
    }
  }

  @Test
  void testSharedInstanceInCentralTenant() {
    String tenantId = "central";
    String centralTenantId = "central";
    mockCommon(tenantId, centralTenantId);

    UUID uuid = UUID.randomUUID();

    try (var is =
        fqmContentFetcher.contents(
            List.of(uuid), EntityType.INSTANCE, List.of(), UUID.randomUUID())) {
      ArgumentCaptor<ContentsRequest> captor = ArgumentCaptor.forClass(ContentsRequest.class);

      await()
          .atMost(2, TimeUnit.SECONDS)
          .untilAsserted(() -> verify(queryClient, atLeastOnce()).getContents(captor.capture()));

      ContentsRequest req = captor.getValue();
      List<List<String>> ids = req.getIds();
      Assertions.assertEquals(1, ids.size());
      Assertions.assertEquals(List.of(uuid.toString(), tenantId), ids.getFirst());
    } catch (IOException e) {
      Assertions.fail("Fail reading content");
    }
  }

  @Test
  void testMemberItemInCentralTenant() {
    String tenantId = "central";
    String centralTenantId = "central";
    String itemTenant1 = "member_A";
    UUID uuid1 = UUID.randomUUID();
    String itemTenant2 = "member_B";
    UUID uuid2 = UUID.randomUUID();
    mockCommon(tenantId, centralTenantId);

    when(searchClient.getConsortiumItemCollection(any()))
        .thenReturn(
            new ConsortiumItemCollection()
                .addItemsItem(new ConsortiumItem().id(uuid1.toString()).tenantId(itemTenant1))
                .addItemsItem(new ConsortiumItem().id(uuid2.toString()).tenantId(itemTenant2)));

    try (var is =
        fqmContentFetcher.contents(
            List.of(uuid1, uuid2), EntityType.ITEM, List.of(), UUID.randomUUID())) {
      ArgumentCaptor<ContentsRequest> captor = ArgumentCaptor.forClass(ContentsRequest.class);

      await()
          .atMost(2, TimeUnit.SECONDS)
          .untilAsserted(() -> verify(queryClient, atLeastOnce()).getContents(captor.capture()));

      ContentsRequest req = captor.getValue();
      List<List<String>> ids = req.getIds();
      Assertions.assertEquals(2, ids.size());
      Assertions.assertEquals(List.of(uuid1.toString(), itemTenant1), ids.getFirst());
      Assertions.assertEquals(List.of(uuid2.toString(), itemTenant2), ids.getLast());
    } catch (IOException e) {
      Assertions.fail("Fail reading content");
    }
  }

  @Test
  void testMemberInstanceInCentralTenant() {
    String tenantId = "member";
    String centralTenantId = "central";
    mockCommon(tenantId, centralTenantId);

    UUID uuid = UUID.randomUUID();

    try (var is =
        fqmContentFetcher.contents(
            List.of(uuid), EntityType.INSTANCE, List.of(), UUID.randomUUID())) {
      ArgumentCaptor<ContentsRequest> captor = ArgumentCaptor.forClass(ContentsRequest.class);

      await()
          .atMost(2, TimeUnit.SECONDS)
          .untilAsserted(() -> verify(queryClient, atLeastOnce()).getContents(captor.capture()));

      ContentsRequest req = captor.getValue();
      List<List<String>> ids = req.getIds();
      Assertions.assertEquals(1, ids.size());
      Assertions.assertEquals(List.of(uuid.toString(), tenantId), ids.getFirst());
    } catch (IOException e) {
      Assertions.fail("Fail reading content");
    }
  }

  @Test
  void testUserCentralTenant() {
    String tenantId = "central";
    String centralTenantId = "central";
    mockCommon(tenantId, centralTenantId);

    UUID uuid = UUID.randomUUID();

    try (var is =
        fqmContentFetcher.contents(List.of(uuid), EntityType.USER, List.of(), UUID.randomUUID())) {
      ArgumentCaptor<ContentsRequest> captor = ArgumentCaptor.forClass(ContentsRequest.class);
      await()
          .atMost(2, TimeUnit.SECONDS)
          .untilAsserted(() -> verify(queryClient, atLeastOnce()).getContents(captor.capture()));

      ContentsRequest req = captor.getValue();
      List<List<String>> ids = req.getIds();
      Assertions.assertEquals(1, ids.size());
      Assertions.assertEquals(List.of(uuid.toString()), ids.getFirst());
    } catch (IOException e) {
      Assertions.fail("Fail reading content");
    }
  }

  private void mockCommon(String tenantId, String centralTenantId) {
    when(folioExecutionContext.getTenantId()).thenReturn(tenantId);
    when(consortiaService.getCentralTenantId(tenantId)).thenReturn(centralTenantId);
    when(entityTypeService.getFqmEntityTypeIdByBulkOpsEntityType(any()))
        .thenReturn(UUID.randomUUID());
    when(queryClient.getContents(any())).thenReturn(List.of(Map.of()));
  }
}
