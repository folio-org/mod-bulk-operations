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
import org.folio.bulkops.domain.dto.EntityType;
import org.folio.bulkops.service.ConsortiaService;
import org.folio.bulkops.service.EntityTypeService;
import org.folio.querytool.domain.dto.ContentsRequest;
import org.folio.spring.FolioExecutionContext;
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
public class FqmContentFetcherEcsTest {

  @MockitoBean private ConsortiaService consortiaService;
  @MockitoBean private EntityTypeService entityTypeService;
  @MockitoBean private FolioExecutionContext folioExecutionContext;
  @MockitoBean private QueryClient queryClient;

  @Value("${application.fqm-fetcher.max_chunk_size}")
  private int chunkSize;

  @Autowired private FqmContentFetcher fqmContentFetcher;

  @Test
  void testCentralTenantBehavior() {
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
  void testConsortiumMemberTenantBehavior() {
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
  void testNonConsortiaTenantBehavior() {
    String tenantId = "regular";
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

  private void mockCommon(String tenantId, String centralTenantId) {
    when(folioExecutionContext.getTenantId()).thenReturn(tenantId);
    when(consortiaService.getCentralTenantId(tenantId)).thenReturn(centralTenantId);
    when(entityTypeService.getFqmEntityTypeIdByBulkOpsEntityType(any()))
        .thenReturn(UUID.randomUUID());
    when(queryClient.getContents(any())).thenReturn(List.of(Map.of()));
  }
}
