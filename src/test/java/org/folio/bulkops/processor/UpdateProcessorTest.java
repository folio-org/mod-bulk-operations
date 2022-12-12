package org.folio.bulkops.processor;

import static org.mockito.Mockito.verify;

import org.folio.bulkops.BaseTest;
import org.folio.bulkops.client.HoldingsClient;
import org.folio.bulkops.client.ItemClient;
import org.folio.bulkops.client.UserClient;
import org.folio.bulkops.domain.dto.ItemLocation;
import org.folio.bulkops.domain.dto.HoldingsRecord;
import org.folio.bulkops.domain.dto.Item;
import org.folio.bulkops.domain.dto.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.UUID;

class UpdateProcessorTest extends BaseTest {
  @Autowired
  private HoldingsUpdateProcessor holdingsUpdateProcessor;
  @MockBean
  private HoldingsClient holdingsClient;

  @Autowired
  private ItemUpdateProcessor itemUpdateProcessor;
  @MockBean
  private ItemClient itemClient;

  @Autowired
  private UserUpdateProcessor userUpdateProcessor;
  @MockBean
  private UserClient userClient;

  @Test
  void shouldUpdateHoldingsRecord() {
    var holdingsRecord = new HoldingsRecord()
        .id(UUID.randomUUID().toString())
        .instanceId(UUID.randomUUID().toString())
        .permanentLocation(new ItemLocation().id(UUID.randomUUID().toString()));

    holdingsUpdateProcessor.updateRecord(holdingsRecord);

    verify(holdingsClient).updateHoldingsRecord(holdingsRecord, holdingsRecord.getId());
  }

  @Test
  void shouldUpdateItem() {
    var item = new Item()
      .id(UUID.randomUUID().toString())
      .holdingsRecordId(UUID.randomUUID().toString());

    itemUpdateProcessor.updateRecord(item);

    verify(itemClient).updateItem(item, item.getId());
  }

  @Test
  void shouldUpdateUser() {
    var user = new User()
        .id(UUID.randomUUID().toString())
        .patronGroup(UUID.randomUUID().toString())
        .username("sample_user");

    userUpdateProcessor.updateRecord(user);

    verify(userClient).updateUser(user, user.getId());
  }
}
