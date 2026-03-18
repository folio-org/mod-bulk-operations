package org.folio.bulkops.util;

import static java.util.UUID.randomUUID;
import static org.awaitility.Awaitility.await;
import static org.folio.bulkops.util.Constants.DUPLICATES_ACROSS_TENANTS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.folio.bulkops.BaseTest;
import org.folio.bulkops.client.QueryClient;
import org.folio.bulkops.client.SearchClient;
import org.folio.bulkops.domain.dto.ConsortiumHolding;
import org.folio.bulkops.domain.dto.ConsortiumHoldingCollection;
import org.folio.bulkops.domain.dto.ConsortiumItem;
import org.folio.bulkops.domain.dto.ConsortiumItemCollection;
import org.folio.bulkops.domain.dto.EntityType;
import org.folio.bulkops.domain.entity.BulkOperationExecutionContent;
import org.folio.bulkops.service.ConsortiaService;
import org.folio.querytool.domain.dto.ContentsRequest;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.client.AuthnClient;
import org.folio.spring.client.PermissionsClient;
import org.folio.spring.client.UsersClient;
import org.folio.spring.service.SystemUserService;
import org.hamcrest.Matchers;
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
    UUID uuid = randomUUID();
    mockCommon(tenantId, centralTenantId, List.of(uuid));

    try (var is =
        fqmContentFetcher.contents(
            List.of(uuid), EntityType.INSTANCE, new ArrayList<>(), randomUUID())) {
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
    UUID uuid = randomUUID();
    mockCommon(tenantId, centralTenantId, List.of(uuid));

    try (var is =
        fqmContentFetcher.contents(
            List.of(uuid), EntityType.INSTANCE, new ArrayList<>(), randomUUID())) {
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
    UUID uuid1 = randomUUID();
    String itemTenant2 = "member_B";
    UUID uuid2 = randomUUID();
    String itemTenant3 = "member_C";
    UUID uuid3 = randomUUID();
    String itemTenant4 = "member_D";
    mockCommon(tenantId, centralTenantId, List.of(uuid1, uuid2));

    when(searchClient.getConsortiumItemCollection(any()))
        .thenReturn(
            new ConsortiumItemCollection()
                .addItemsItem(new ConsortiumItem().id(uuid1.toString()).tenantId(itemTenant1))
                .addItemsItem(new ConsortiumItem().id(uuid2.toString()).tenantId(itemTenant2))
                .addItemsItem(new ConsortiumItem().id(uuid3.toString()).tenantId(itemTenant3))
                // Duplicate across tenants case - 1 item in two tenants
                .addItemsItem(new ConsortiumItem().id(uuid3.toString()).tenantId(itemTenant4)));

    List<BulkOperationExecutionContent> bulkOperationExecutionContents = new ArrayList<>();
    try (var is =
        fqmContentFetcher.contents(
            List.of(uuid1, uuid2), EntityType.ITEM, bulkOperationExecutionContents, randomUUID())) {
      ArgumentCaptor<ContentsRequest> captor = ArgumentCaptor.forClass(ContentsRequest.class);

      await()
          .atMost(2, TimeUnit.SECONDS)
          .untilAsserted(() -> verify(queryClient, atLeastOnce()).getContents(captor.capture()));

      assertThat(bulkOperationExecutionContents, Matchers.hasSize(1));
      assertThat(
          bulkOperationExecutionContents.getFirst().getErrorMessage(),
          Matchers.is(DUPLICATES_ACROSS_TENANTS));

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
  void testMemberHoldingInCentralTenant() {
    String tenantId = "central";
    String centralTenantId = "central";
    String itemTenant1 = "member_A";
    UUID uuid1 = randomUUID();
    String itemTenant2 = "member_B";
    UUID uuid2 = randomUUID();
    String itemTenant3 = "member_C";
    UUID uuid3 = randomUUID();
    String itemTenant4 = "member_D";
    mockCommon(tenantId, centralTenantId, List.of(uuid1, uuid2));

    when(searchClient.getConsortiumHoldingCollection(any()))
        .thenReturn(
            new ConsortiumHoldingCollection()
                .addHoldingsItem(new ConsortiumHolding().id(uuid1.toString()).tenantId(itemTenant1))
                .addHoldingsItem(new ConsortiumHolding().id(uuid2.toString()).tenantId(itemTenant2))
                .addHoldingsItem(new ConsortiumHolding().id(uuid3.toString()).tenantId(itemTenant3))
                // Duplicate across tenants case - 1 holding in two tenants
                .addHoldingsItem(
                    new ConsortiumHolding().id(uuid3.toString()).tenantId(itemTenant4)));

    List<BulkOperationExecutionContent> bulkOperationExecutionContents = new ArrayList<>();
    try (var is =
        fqmContentFetcher.contents(
            List.of(uuid1, uuid2),
            EntityType.HOLDINGS_RECORD,
            bulkOperationExecutionContents,
            randomUUID())) {
      ArgumentCaptor<ContentsRequest> captor = ArgumentCaptor.forClass(ContentsRequest.class);

      await()
          .atMost(2, TimeUnit.SECONDS)
          .untilAsserted(() -> verify(queryClient, atLeastOnce()).getContents(captor.capture()));

      assertThat(bulkOperationExecutionContents, Matchers.hasSize(1));
      assertThat(
          bulkOperationExecutionContents.getFirst().getErrorMessage(),
          Matchers.is(DUPLICATES_ACROSS_TENANTS));

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
    UUID uuid = randomUUID();
    mockCommon(tenantId, centralTenantId, List.of(uuid));

    try (var is =
        fqmContentFetcher.contents(
            List.of(uuid), EntityType.INSTANCE, new ArrayList<>(), randomUUID())) {
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
    UUID uuid = randomUUID();
    mockCommon(tenantId, centralTenantId, List.of(uuid));

    try (var is =
        fqmContentFetcher.contents(
            List.of(uuid), EntityType.USER, new ArrayList<>(), randomUUID())) {
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

  private void mockCommon(String tenantId, String centralTenantId, List<UUID> uuids) {
    when(folioExecutionContext.getTenantId()).thenReturn(tenantId);
    when(consortiaService.getCentralTenantId(tenantId)).thenReturn(centralTenantId);

    List<Map<String, Object>> mock = new ArrayList<>();

    /* Adding entities ids all together for mock - just emulation */
    uuids.forEach(
        uuid -> {
          Map<String, Object> map = new HashMap<>();
          map.put("instance.id", uuid.toString());
          map.put("users.id", uuid.toString());
          map.put("items.id", uuid.toString());
          map.put("holdings.id", uuid.toString());
          map.put("instance.tenant_id", "some-tenant");
          map.put("users.tenant_id", "some-tenant");
          map.put("items.tenant_id", "some-tenant");
          map.put("holdings.tenant_id", "some-tenant");
          map.put("instance.jsonb", "{\"id\": \"" + uuid + "\"}");
          map.put("users.jsonb", "{\"id\": \"" + uuid + "\"}");
          map.put("items.jsonb", "{\"id\": \"" + uuid + "\"}");
          map.put("holdings.jsonb", "{\"id\": \"" + uuid + "\"}");
          mock.add(map);
        });

    when(queryClient.getContents(any())).thenReturn(mock);
  }
}
