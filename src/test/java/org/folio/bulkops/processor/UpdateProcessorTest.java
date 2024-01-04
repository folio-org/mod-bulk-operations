package org.folio.bulkops.processor;

import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.folio.bulkops.BaseTest;
import org.folio.bulkops.client.ItemClient;
import org.folio.bulkops.domain.bean.HoldingsRecord;
import org.folio.bulkops.domain.bean.Instance;
import org.folio.bulkops.domain.bean.Item;
import org.folio.bulkops.domain.bean.User;
import org.folio.bulkops.domain.dto.BulkOperationRuleCollection;
import org.folio.bulkops.service.ErrorService;
import org.folio.bulkops.service.RuleService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;

class UpdateProcessorTest extends BaseTest {

  @MockBean
  private RuleService ruleService;
  @SpyBean
  private ItemClient itemClient;
  @SpyBean
  private ErrorService errorService;
  @Autowired
  private HoldingsUpdateProcessor holdingsUpdateProcessor;
  @Autowired
  private ItemUpdateProcessor itemUpdateProcessor;
  @Autowired
  private UserUpdateProcessor userUpdateProcessor;
  @Autowired
  private InstanceUpdateProcessor instanceUpdateProcessor;

  @Test
  void shouldUpdateHoldingsRecord() {
    var holdingsRecord = new HoldingsRecord()
        .withId(UUID.randomUUID().toString())
        .withInstanceId(UUID.randomUUID().toString());

    when(ruleService.getRules(isA(UUID.class))).thenReturn(new BulkOperationRuleCollection());
    holdingsUpdateProcessor.updateRecord(holdingsRecord,"identifier", UUID.randomUUID());

    verify(holdingsClient).updateHoldingsRecord(holdingsRecord, holdingsRecord.getId());
  }

  @Test
  void shouldUpdateItem() {
    var item = new Item()
      .withId(UUID.randomUUID().toString())
      .withHoldingsRecordId(UUID.randomUUID().toString());

    itemUpdateProcessor.updateRecord(item, "identifier", UUID.randomUUID());

    verify(itemClient).updateItem(item, item.getId());
  }

  @Test
  void shouldUpdateUser() {
    var user = new User()
        .withId(UUID.randomUUID().toString())
        .withPatronGroup(UUID.randomUUID().toString())
        .withUsername("sample_user");

    userUpdateProcessor.updateRecord(user, "identifier",  UUID.randomUUID());

    verify(userClient).updateUser(user, user.getId());
  }

  @Test
  void shouldUpdateInstance() {
    var instance = Instance.builder()
      .id(UUID.randomUUID().toString())
      .title("Title")
      .build();

    instanceUpdateProcessor.updateRecord(instance, "identifier", UUID.randomUUID());

    verify(instanceStorageClient).updateInstance(instance, instance.getId());
  }
}
