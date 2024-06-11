package org.folio.bulkops.util;

import org.folio.bulkops.BaseTest;
import org.folio.bulkops.domain.bean.HoldingsRecord;
import org.folio.bulkops.domain.bean.Instance;
import org.folio.bulkops.domain.bean.Item;
import org.folio.bulkops.domain.bean.User;
import org.folio.bulkops.domain.dto.EntityType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class EntityPathResolverTest extends BaseTest {

  @Autowired
  private EntityPathResolver entityPathResolver;

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
    var actual = entityPathResolver.resolve(EntityType.INSTANCE, entity);
    var expected = format("/inventory/view/%s", entity.getId());
    assertThat(expected).isEqualTo(actual);
  }

  @Test
  void testHoldingPathResolving() {
    var entity = HoldingsRecord.builder().id(UUID.randomUUID().toString()).instanceId(UUID.randomUUID().toString()).build();
    var actual = entityPathResolver.resolve(EntityType.HOLDINGS_RECORD, entity);
    var expected = format("/inventory/view/%s/%s", entity.getInstanceId(), entity.getId());
    assertThat(expected).isEqualTo(actual);
  }

  @Test
  void testItemPathResolving() {
    var entity = Item.builder().id(UUID.randomUUID().toString()).holdingsRecordId(UUID.randomUUID().toString()).build();
    var instanceId = UUID.randomUUID().toString();
    when(holdingsClient.getHoldingById(entity.getHoldingsRecordId())).thenReturn(HoldingsRecord.builder().instanceId(instanceId).build());
    var actual = entityPathResolver.resolve(EntityType.ITEM, entity);
    var expected = format("/inventory/view/%s/%s/%s", instanceId, entity.getHoldingsRecordId(), entity.getId());
    assertThat(expected).isEqualTo(actual);
  }
}
