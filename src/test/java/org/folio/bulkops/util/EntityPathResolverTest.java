package org.folio.bulkops.util;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.UUID;
import org.folio.bulkops.BaseTest;
import org.folio.bulkops.domain.bean.ExtendedHoldingsRecord;
import org.folio.bulkops.domain.bean.ExtendedInstance;
import org.folio.bulkops.domain.bean.ExtendedItem;
import org.folio.bulkops.domain.bean.HoldingsRecord;
import org.folio.bulkops.domain.bean.Instance;
import org.folio.bulkops.domain.bean.Item;
import org.folio.bulkops.domain.bean.User;
import org.folio.bulkops.domain.dto.EntityType;
import org.folio.spring.scope.FolioExecutionContextSetter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;

class EntityPathResolverTest extends BaseTest {

  @Autowired private EntityPathResolver entityPathResolver;

  @Test
  void testUserPathResolving() {
    var entity = User.builder().id(UUID.randomUUID().toString()).build();
    var actual = entityPathResolver.resolve(EntityType.USER, entity);
    var expected = format("/users/%s", entity.getId());

    assertThat(expected).isEqualTo(actual);
  }

  @ParameterizedTest
  @EnumSource(
      value = EntityType.class,
      names = {"INSTANCE", "INSTANCE_MARC"})
  void testInstancePathResolving(EntityType entityType) {
    var entity = Instance.builder().id(UUID.randomUUID().toString()).build();
    final var extendedInstance =
        ExtendedInstance.builder().entity(entity).tenantId("tenantId").build();

    var actual = entityPathResolver.resolve(entityType, extendedInstance);
    var expected = format("/inventory/view/%s", entity.getId());
    assertThat(expected).isEqualTo(actual);
  }

  @Test
  void testHoldingPathResolving() {
    var entity =
        HoldingsRecord.builder()
            .id(UUID.randomUUID().toString())
            .instanceId(UUID.randomUUID().toString())
            .build();
    final var extendedEntity =
        ExtendedHoldingsRecord.builder().entity(entity).tenantId("tenantId").build();

    var actual = entityPathResolver.resolve(EntityType.HOLDINGS_RECORD, extendedEntity);
    var expected = format("/inventory/view/%s/%s", entity.getInstanceId(), entity.getId());
    assertThat(expected).isEqualTo(actual);
  }

  @Test
  void testItemPathResolving() {
    try (var context = new FolioExecutionContextSetter(folioExecutionContext)) {
      var entity =
          Item.builder()
              .id(UUID.randomUUID().toString())
              .holdingsRecordId(UUID.randomUUID().toString())
              .build();
      final var extendedEntity = ExtendedItem.builder().entity(entity).tenantId("tenantId").build();

      final var instanceId = UUID.randomUUID().toString();
      when(holdingsStorageClient.getHoldingById(entity.getHoldingsRecordId()))
          .thenReturn(HoldingsRecord.builder().instanceId(instanceId).build());

      var actual = entityPathResolver.resolve(EntityType.ITEM, extendedEntity);
      var expected =
          format(
              "/inventory/view/%s/%s/%s", instanceId, entity.getHoldingsRecordId(), entity.getId());
      assertThat(expected).isEqualTo(actual);
    }
  }
}
