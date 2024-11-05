package org.folio.bulkops.util;

import org.folio.bulkops.BaseTest;
import org.folio.bulkops.domain.bean.ExtendedHoldingsRecord;
import org.folio.bulkops.domain.bean.ExtendedInstance;
import org.folio.bulkops.domain.bean.ExtendedItem;
import org.folio.bulkops.domain.bean.HoldingsRecord;
import org.folio.bulkops.domain.bean.Instance;
import org.folio.bulkops.domain.bean.Item;
import org.folio.bulkops.domain.bean.User;
import org.folio.bulkops.domain.dto.EntityType;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.integration.XOkapiHeaders;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class EntityPathResolverTest extends BaseTest {

  @Autowired
  private EntityPathResolver entityPathResolver;
  @SpyBean
  private FolioExecutionContext folioExecutionContext;

  @Test
  void testUserPathResolving() {
    var entity = User.builder().id(UUID.randomUUID().toString()).build();
    var actual = entityPathResolver.resolve(EntityType.USER, entity);
    var expected = format("/users/%s", entity.getId());
    assertThat(expected).isEqualTo(actual);
  }

  @Test
  void testInstancePathResolving() {
    var entity = Instance.builder().id(UUID.randomUUID().toString()).build();
    var extendedInstance = ExtendedInstance.builder().entity(entity).tenantId("tenantId").build();
    var actual = entityPathResolver.resolve(EntityType.INSTANCE, extendedInstance);
    var expected = format("/inventory/view/%s", entity.getId());
    assertThat(expected).isEqualTo(actual);
  }

  @Test
  void testHoldingPathResolving() {
    var entity = HoldingsRecord.builder().id(UUID.randomUUID().toString()).instanceId(UUID.randomUUID().toString()).build();
    var extendedEntity = ExtendedHoldingsRecord.builder().entity(entity).tenantId("tenantId").build();
    var actual = entityPathResolver.resolve(EntityType.HOLDINGS_RECORD, extendedEntity);
    var expected = format("/inventory/view/%s/%s", entity.getInstanceId(), entity.getId());
    assertThat(expected).isEqualTo(actual);
  }

  @Test
  void testItemPathResolving() {
    var entity = Item.builder().id(UUID.randomUUID().toString()).holdingsRecordId(UUID.randomUUID().toString()).build();
    var extendedEntity = ExtendedItem.builder().entity(entity).tenantId("tenantId").build();
    var instanceId = UUID.randomUUID().toString();
    var tenantId = "diku";
    HashMap<String, Collection<String>> headers = new HashMap<>();
    headers.put(XOkapiHeaders.TENANT, List.of(tenantId));

    when(folioExecutionContext.getOkapiHeaders()).thenReturn(headers);
    when(folioExecutionContext.getAllHeaders()).thenReturn(headers);
    when(holdingsClient.getHoldingById(entity.getHoldingsRecordId())).thenReturn(HoldingsRecord.builder().instanceId(instanceId).build());

    var actual = entityPathResolver.resolve(EntityType.ITEM, extendedEntity);
    var expected = format("/inventory/view/%s/%s/%s", instanceId, entity.getHoldingsRecordId(), entity.getId());
    assertThat(expected).isEqualTo(actual);
  }
}
